package com.pedidos360.reporting_service.config;

import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import com.pedidos360.reporting_service.dto.StockEventoKafka;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.Map;

/**
 * Fábricas de consumidores, una por tipo de evento.
 *
 * Kafka guarda el mensaje como bytes; para convertirlo a objeto se necesita
 * un deserializador que sepa la clase destino. Como los productores no envían
 * cabeceras de tipo (spring.json.add.type.headers=false), aquí indicamos
 * explícitamente PedidoEventoKafka o StockEventoKafka.
 *
 * Tener dos factories evita ambigüedad: el método @KafkaListener elige la
 * factory con el atributo containerFactory.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventoKafka> pedidoListenerContainerFactory(
            KafkaProperties kafkaProperties) {

        return crearFactory(kafkaProperties, PedidoEventoKafka.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockEventoKafka> stockListenerContainerFactory(
            KafkaProperties kafkaProperties) {

        return crearFactory(kafkaProperties, StockEventoKafka.class);
    }

    /**
     * Construye la factory con las propiedades de spring.kafka.consumer.*
     * y un JacksonJsonDeserializer atado a la clase del evento.
     *
     * El segundo argumento false significa "no leer el tipo desde cabeceras";
     * siempre se deserializa a la clase indicada.
     */
    private <T> ConcurrentKafkaListenerContainerFactory<String, T> crearFactory(
            KafkaProperties kafkaProperties, Class<T> tipoEvento) {

        Map<String, Object> propiedades = kafkaProperties.buildConsumerProperties();
        JacksonJsonDeserializer<T> deserializador = new JacksonJsonDeserializer<>(tipoEvento, false);

        DefaultKafkaConsumerFactory<String, T> consumerFactory =
                new DefaultKafkaConsumerFactory<>(propiedades, new StringDeserializer(), deserializador);

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
