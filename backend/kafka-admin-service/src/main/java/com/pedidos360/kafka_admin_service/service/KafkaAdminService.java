package com.pedidos360.kafka_admin_service.service;

import com.pedidos360.kafka_admin_service.dto.ConsumerGroupInfo;
import com.pedidos360.kafka_admin_service.dto.OffsetParticion;
import com.pedidos360.kafka_admin_service.dto.ParticionInfo;
import com.pedidos360.kafka_admin_service.dto.TopicoConfigRequest;
import com.pedidos360.kafka_admin_service.dto.TopicoInfo;
import com.pedidos360.kafka_admin_service.dto.TopicoRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Lógica de administración de Kafka usando AdminClient.
 *
 * Cada método arma la solicitud con las clases del cliente (NewTopic,
 * ConfigResource, AlterConfigOp, etc.) y espera el resultado con el helper
 * esperar(...), que convierte los errores de Kafka en excepciones simples
 * para que el controller las traduzca a códigos HTTP.
 */
@Service
public class KafkaAdminService {

    private static final long TIMEOUT_SEGUNDOS = 10;

    private final AdminClient adminClient;

    public KafkaAdminService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    /** Crea un tópico con particiones, réplicas y configuraciones iniciales. */
    public void crearTopico(TopicoRequest request) {
        Map<String, String> configuraciones = new HashMap<>();
        configuraciones.put("retention.ms", String.valueOf(request.getRetentionMs()));
        configuraciones.put("cleanup.policy", request.getCleanupPolicy());
        configuraciones.put("min.insync.replicas", String.valueOf(request.getMinInSyncReplicas()));

        NewTopic nuevo = new NewTopic(
                request.getNombre(),
                request.getParticiones(),
                request.getReplicas())
                .configs(configuraciones);

        esperar(adminClient.createTopics(List.of(nuevo)).all());
    }

    /** Lista los nombres de todos los tópicos del clúster, ordenados. */
    public List<String> listarTopicos() {
        Set<String> nombres = esperar(adminClient.listTopics().names());
        List<String> ordenados = new ArrayList<>(nombres);
        Collections.sort(ordenados);
        return ordenados;
    }

    /** Describe un tópico: particiones, líder, réplicas, ISR y configuraciones. */
    public TopicoInfo describirTopico(String nombre) {
        exigirTopicoExistente(nombre);

        Map<String, TopicDescription> descripciones =
                esperar(adminClient.describeTopics(List.of(nombre)).allTopicNames());
        TopicDescription descripcion = descripciones.get(nombre);
        if (descripcion == null) {
            throw new NoSuchElementException("Tópico no encontrado: " + nombre);
        }

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(adminClient.describeConfigs(List.of(recurso)).all()).get(recurso);

        Map<String, String> configuraciones = new TreeMap<>();
        if (config != null) {
            for (ConfigEntry entrada : config.entries()) {
                configuraciones.put(entrada.name(), entrada.value());
            }
        }

        List<ParticionInfo> particiones = new ArrayList<>();
        for (TopicPartitionInfo p : descripcion.partitions()) {
            Integer lider = p.leader() != null ? p.leader().id() : null;
            List<Integer> replicas = p.replicas().stream().map(Node::id).toList();
            List<Integer> isr = p.isr().stream().map(Node::id).toList();
            particiones.add(new ParticionInfo(p.partition(), lider, replicas, isr));
        }

        int replicas = particiones.isEmpty() ? 0 : particiones.get(0).getReplicas().size();
        return new TopicoInfo(nombre, particiones.size(), replicas, particiones, configuraciones);
    }

    /** Modifica retention.ms y/o cleanup.policy de un tópico existente. */
    public void modificarConfiguracion(String nombre, TopicoConfigRequest request) {
        Map<String, String> cambios = new HashMap<>();
        if (request.getRetentionMs() != null) {
            cambios.put("retention.ms", String.valueOf(request.getRetentionMs()));
        }
        if (request.getCleanupPolicy() != null && !request.getCleanupPolicy().isBlank()) {
            cambios.put("cleanup.policy", request.getCleanupPolicy());
        }
        if (cambios.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar retentionMs o cleanupPolicy");
        }

        exigirTopicoExistente(nombre);

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        List<AlterConfigOp> operaciones = new ArrayList<>();
        for (Map.Entry<String, String> cambio : cambios.entrySet()) {
            operaciones.add(new AlterConfigOp(
                    new ConfigEntry(cambio.getKey(), cambio.getValue()),
                    AlterConfigOp.OpType.SET));
        }

        esperar(adminClient.incrementalAlterConfigs(Map.of(recurso, operaciones)).all());
    }

    /** Elimina un tópico. */
    public void eliminarTopico(String nombre) {
        exigirTopicoExistente(nombre);
        esperar(adminClient.deleteTopics(List.of(nombre)).all());
    }

    /** Lista todos los consumer groups con sus offsets y lag por partición. */
    public List<ConsumerGroupInfo> listarGruposConLag() {
        Collection<ConsumerGroupListing> grupos = esperar(adminClient.listConsumerGroups().all());
        List<ConsumerGroupInfo> resultado = new ArrayList<>();
        for (ConsumerGroupListing grupo : grupos) {
            resultado.add(describirGrupo(grupo.groupId()));
        }
        return resultado;
    }

    private ConsumerGroupInfo describirGrupo(String groupId) {
        ConsumerGroupDescription descripcion = esperar(
                adminClient.describeConsumerGroups(List.of(groupId)).all()).get(groupId);

        Map<TopicPartition, OffsetAndMetadata> offsets = esperar(
                adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());

        Map<TopicPartition, OffsetSpec> consultas = new HashMap<>();
        for (TopicPartition tp : offsets.keySet()) {
            consultas.put(tp, OffsetSpec.latest());
        }

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales = consultas.isEmpty()
                ? Map.of()
                : esperar(adminClient.listOffsets(consultas).all());

        List<OffsetParticion> detalle = new ArrayList<>();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entrada : offsets.entrySet()) {
            TopicPartition tp = entrada.getKey();
            long actual = entrada.getValue().offset();
            ListOffsetsResult.ListOffsetsResultInfo info = finales.get(tp);
            long fin = info != null ? info.offset() : -1;
            long lag = fin >= 0 ? Math.max(0, fin - actual) : -1;
            detalle.add(new OffsetParticion(tp.topic(), tp.partition(), actual, fin, lag));
        }

        String estado = descripcion != null ? descripcion.state().toString() : "DESCONOCIDO";
        int miembros = descripcion != null ? descripcion.members().size() : 0;
        return new ConsumerGroupInfo(groupId, estado, miembros, detalle);
    }

    private void exigirTopicoExistente(String nombre) {
        if (!listarTopicos().contains(nombre)) {
            throw new NoSuchElementException("Tópico no encontrado: " + nombre);
        }
    }

    /**
     * Espera el resultado de una operación de Kafka con timeout.
     * Convierte las excepciones checked en RuntimeException para no ensuciar
     * los métodos del servicio.
     */
    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida contra Kafka", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Error al comunicarse con Kafka: " + e.getMessage(), e);
        }
    }
}
