package com.pedidos360.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entrada de la línea de tiempo de auditoría.
 *
 * Conserva los datos del evento del pedido y agrega recibidoEn, el momento
 * en que reporting-service lo consumió. Así se puede contrastar cuándo se
 * generó el evento y cuándo llegó.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoAuditoria {

    private Long pedidoId;
    private String estado;
    private String cliente;
    private Long productoId;
    private int cantidad;
    private LocalDateTime timestamp;
    private LocalDateTime recibidoEn;
}
