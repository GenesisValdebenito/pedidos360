package com.pedidos360.notifications_service.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "pedidos360.direct.exchange";
    public static final String NOTIFICACIONES_QUEUE = "notificaciones.queue";

    public static final String PEDIDO_CREADO = "pedido.creado";
    public static final String PEDIDO_ACEPTADO = "pedido.aceptado";
    public static final String PEDIDO_EN_PREPARACION = "pedido.en_preparacion";
    public static final String PEDIDO_DESPACHADO = "pedido.despachado";
    public static final String PEDIDO_ENTREGADO = "pedido.entregado";
    public static final String PEDIDO_CANCELADO = "pedido.cancelado";

    @Bean
    public DirectExchange pedidosExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(NOTIFICACIONES_QUEUE).build();
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
