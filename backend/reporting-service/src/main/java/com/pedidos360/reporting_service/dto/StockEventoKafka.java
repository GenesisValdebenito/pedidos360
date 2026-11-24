package com.pedidos360.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Copia del evento que publica catalog-service en "stock.eventos".
 * El tópico está compactado por productoId, así que cada mensaje representa
 * el último stock conocido del producto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockEventoKafka {

    private Long productoId;
    private String nombre;
    private int stock;
    private int precio;
    private LocalDateTime timestamp;
}
