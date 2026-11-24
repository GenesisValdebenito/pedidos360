package com.pedidos360.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Copia del evento que publica orders-service en "pedidos.eventos".
 *
 * Cada microservicio define su propia copia del DTO: no hay módulo común.
 * Los nombres de campos deben coincidir con el JSON que viaja por Kafka.
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
