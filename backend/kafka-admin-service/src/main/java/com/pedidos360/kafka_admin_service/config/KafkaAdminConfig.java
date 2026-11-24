package com.pedidos360.kafka_admin_service.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuración del cliente administrativo de Kafka.
 *
 * AdminClient es el componente de kafka-clients que permite crear/eliminar
 * tópicos, describir configuraciones y consultar consumer groups. No produce
 * ni consume mensajes: solo administra el clúster.
 *
 * Se declara como bean para inyectarlo en KafkaAdminService. destroyMethod
 * cierra las conexiones cuando la aplicación termina.
 */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }
}
