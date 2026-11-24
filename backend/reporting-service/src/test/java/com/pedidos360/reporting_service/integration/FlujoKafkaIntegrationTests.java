package com.pedidos360.reporting_service.integration;

import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import com.pedidos360.reporting_service.dto.StockEventoKafka;
import com.pedidos360.reporting_service.service.AuditoriaService;
import com.pedidos360.reporting_service.service.KpiService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración real: un broker Kafka embebido, un productor con el
 * mismo JacksonJsonSerializer que usan orders/catalog, los @KafkaListener
 * reales del servicio y los dos consumer groups.
 *
 * No usa la infraestructura Docker ni la base de datos: el broker se levanta
 * dentro de la JVM. Los tópicos se crean con 3 particiones, igual que en infra/.
 */
@SpringBootTest(properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.enable-auto-commit=false"
})
@EmbeddedKafka(
        partitions = 3,
        topics = {"pedidos.eventos", "stock.eventos"})
class FlujoKafkaIntegrationTests {

    private static final long ESPERA_MS = 30_000;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KpiService kpiService;

    @Autowired
    private AuditoriaService auditoriaService;

    /**
     * El contexto levanta SecurityConfig, que necesita un JwtDecoder.
     * En esta prueba no se valida ningún token, así que se registra uno que
     * siempre rechaza; así no se depende de Entra ID.
     */
    @TestConfiguration
    static class SeguridadDePrueba {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new JwtException("Token no válido en pruebas");
            };
        }
    }

    /**
     * Hace que los consumidores del contexto apunten al broker embebido.
     * @EmbeddedKafka publica la dirección en esta propiedad del sistema.
     */
    @DynamicPropertySource
    static void apuntarAlBrokerEmbebido(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Test
    void elFlujoProductorTopicoConsumidorActualizaReportesYDejaLagCero() throws Exception {
        KafkaTemplate<String, Object> productor = crearProductor();

        productor.send("pedidos.eventos", "1", new PedidoEventoKafka(
                1L, "CREADO", "cliente@ejemplo.com", 20L, 2,
                LocalDateTime.now())).get(10, TimeUnit.SECONDS);

        productor.send("pedidos.eventos", "2", new PedidoEventoKafka(
                2L, "CREADO", "otro@ejemplo.com", 20L, 3,
                LocalDateTime.now())).get(10, TimeUnit.SECONDS);

        productor.send("pedidos.eventos", "2", new PedidoEventoKafka(
                2L, "CANCELADO", "otro@ejemplo.com", 20L, 3,
                LocalDateTime.now())).get(10, TimeUnit.SECONDS);

        productor.send("stock.eventos", "20", new StockEventoKafka(
                20L, "Teclado", 5, 1500,
                LocalDateTime.now())).get(10, TimeUnit.SECONDS);

        esperar("KPIs actualizados", () -> {
            var kpis = kpiService.obtener();
            return kpis.getPedidosCreados() == 2
                    && kpis.getUnidadesSolicitadas() == 5
                    && Long.valueOf(1L).equals(kpis.getPedidosPorEstado().get("CANCELADO"));
        });

        esperar("auditoría con los 3 eventos", () ->
                auditoriaService.obtenerLineaTiempo(10).size() == 3);

        esperar("stock actualizado", () -> {
            var stock = auditoriaService.buscarStock(20L);
            return stock != null && stock.getStock() == 5;
        });

        var kpis = kpiService.obtener();
        assertEquals(2, kpis.getPedidosCreados());
        assertEquals(5, kpis.getUnidadesSolicitadas());
        assertEquals(50.0, kpis.getTasaCancelacionPorcentaje());
        assertEquals(2.5, kpis.getPromedioUnidadesPorPedido());

        assertTrue(auditoriaService.obtenerLineaTiempo(10).stream()
                .anyMatch(evento -> "CANCELADO".equals(evento.getEstado())));

        try (AdminClient admin = crearAdmin()) {
            esperar("los dos grupos registrados", () -> {
                try {
                    Set<String> grupos = admin.listConsumerGroups().all()
                            .get(10, TimeUnit.SECONDS).stream()
                            .map(ConsumerGroupListing::groupId)
                            .collect(Collectors.toSet());
                    return grupos.contains("reporteria-group")
                            && grupos.contains("auditoria-group");
                } catch (Exception e) {
                    return false;
                }
            });

            esperar("lag cero en reporteria-group", () -> lagTotal(admin, "reporteria-group") == 0);
            esperar("lag cero en auditoria-group", () -> lagTotal(admin, "auditoria-group") == 0);

            assertEquals(0, lagTotal(admin, "reporteria-group"));
            assertEquals(0, lagTotal(admin, "auditoria-group"));
        }
    }

    private KafkaTemplate<String, Object> crearProductor() {
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        propiedades.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propiedades.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        propiedades.put("spring.json.add.type.headers", false);

        ProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(propiedades);
        return new KafkaTemplate<>(factory);
    }

    private AdminClient crearAdmin() {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()));
    }

    /**
     * Suma el lag de todas las particiones del grupo: último offset del tópico
     * menos el offset confirmado. Devuelve -1 mientras el grupo no tenga offsets.
     */
    private long lagTotal(AdminClient admin, String grupo) {
        try {
            Map<TopicPartition, OffsetAndMetadata> offsets = admin
                    .listConsumerGroupOffsets(grupo)
                    .partitionsToOffsetAndMetadata()
                    .get(10, TimeUnit.SECONDS);

            if (offsets.isEmpty()) {
                return -1;
            }

            Map<TopicPartition, OffsetSpec> consultas = new HashMap<>();
            for (TopicPartition tp : offsets.keySet()) {
                consultas.put(tp, OffsetSpec.latest());
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales = admin
                    .listOffsets(consultas)
                    .all()
                    .get(10, TimeUnit.SECONDS);

            long lag = 0;
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entrada : offsets.entrySet()) {
                long fin = finales.get(entrada.getKey()).offset();
                lag += Math.max(0, fin - entrada.getValue().offset());
            }
            return lag;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Reintenta una condición hasta que se cumpla o venza el tiempo. */
    private void esperar(String descripcion, BooleanSupplier condicion) {
        long limite = System.currentTimeMillis() + ESPERA_MS;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Espera interrumpida: " + descripcion, e);
            }
        }
        throw new AssertionError("Se agotó el tiempo esperando: " + descripcion);
    }
}
