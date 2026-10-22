package com.pedidos360.orders_service.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "pedidos360.direct.exchange";

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

    public static String routingKeyParaEstado(String estado) {
        return switch (estado) {
            case "CREADO" -> PEDIDO_CREADO;
            case "ACEPTADO" -> PEDIDO_ACEPTADO;
            case "EN_PREPARACION" -> PEDIDO_EN_PREPARACION;
            case "DESPACHADO" -> PEDIDO_DESPACHADO;
            case "ENTREGADO" -> PEDIDO_ENTREGADO;
            case "CANCELADO" -> PEDIDO_CANCELADO;
            default -> throw new IllegalArgumentException("Estado de pedido desconocido: " + estado);
        };
    }
}
