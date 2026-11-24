package com.pedidos360.catalog_service.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento de stock que se publica en Kafka (tópico "stock.eventos").
 *
 * Representa el estado completo del producto en el momento del cambio
 * (creación, actualización o descuento). El tópico está compactado y usa
 * productoId como clave, así que Kafka conserva solo el último evento de
 * cada producto.
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
