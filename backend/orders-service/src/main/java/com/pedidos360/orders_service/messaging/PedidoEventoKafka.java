package com.pedidos360.orders_service.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento de pedido que se publica en Kafka (tópico "pedidos.eventos").
 *
 * Es distinto del PedidoEvento de RabbitMQ: Kafka necesita más datos para
 * que reporting-service pueda calcular KPIs (producto y cantidad) sin tener
 * que consultar la base de datos de orders-service.
 *
 * El campo estado es el que dispara cada paso del pedido: CREADO, ACEPTADO,
 * EN_PREPARACION, DESPACHADO, ENTREGADO o CANCELADO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEventoKafka {

    private Long pedidoId;
    private String estado;
    private String cliente;
    private Long productoId;
    private int cantidad;
    private LocalDateTime timestamp;
}
