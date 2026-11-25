package com.pedidos360.notifications_service.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "pedidos360.direct.exchange";
    public static final String NOTIFICACIONES_QUEUE = "notificaciones.queue";

    // Dead Letter Exchange / Queue: a dónde van los mensajes que fallan.
    public static final String NOTIFICACIONES_DLX = "pedidos360.direct.dlx";
    public static final String NOTIFICACIONES_DLQ = "notificaciones.dlq";
    public static final String DLQ_ROUTING_KEY = "notificacion.fallida";

    public static final String PEDIDO_CREADO = "pedido.creado";
    public static final String PEDIDO_ACEPTADO = "pedido.aceptado";
    public static final String PEDIDO_EN_PREPARACION = "pedido.en_preparacion";
    public static final String PEDIDO_DESPACHADO = "pedido.despachado";
    public static final String PEDIDO_ENTREGADO = "pedido.entregado";
    public static final String PEDIDO_CANCELADO = "pedido.cancelado";

    /**
     * Spring Boot 4 ya no registra RabbitAdmin automáticamente, así que lo
     * declaramos sobre la ConnectionFactory configurada por spring.rabbitmq.*.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public DirectExchange pedidosExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Cola principal de notificaciones. Está suscrita al DLX: cuando un
     * mensaje recibe un NACK con requeue=false (o expira/rechaza), RabbitMQ
     * lo reenvía automáticamente a NOTIFICACIONES_DLX.
     */
    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(NOTIFICACIONES_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICACIONES_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /** Dead Letter Exchange que recibe los mensajes fallidos. */
    @Bean
    public DirectExchange pedidosDlx() {
        return ExchangeBuilder.directExchange(NOTIFICACIONES_DLX)
                .durable(true)
                .build();
    }

    /** Dead Letter Queue: aquí quedan los mensajes que no se pudieron procesar. */
    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(NOTIFICACIONES_DLQ).build();
    }

    @Bean
    public Binding notificacionesDlqBinding(Queue notificacionesDlq, DirectExchange pedidosDlx) {
        return BindingBuilder.bind(notificacionesDlq).to(pedidosDlx).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding pedidoCreadoBinding(Queue notificacionesQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pedidosExchange).with(PEDIDO_CREADO);
    }

    @Bean
    public Binding pedidoAceptadoBinding(Queue notificacionesQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pedidosExchange).with(PEDIDO_ACEPTADO);
    }

    @Bean
    public Binding pedidoEnPreparacionBinding(Queue notificacionesQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pedidosExchange).with(PEDIDO_EN_PREPARACION);
    }

    @Bean
    public Binding pedidoDespachadoBinding(Queue notificacionesQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pedidosExchange).with(PEDIDO_DESPACHADO);
    }

    @Bean
    public Binding pedidoEntregadoBinding(Queue notificacionesQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pedidosExchange).with(PEDIDO_ENTREGADO);
    }

    @Bean
    public Binding pedidoCanceladoBinding(Queue notificacionesQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(pedidosExchange).with(PEDIDO_CANCELADO);
    }
}
