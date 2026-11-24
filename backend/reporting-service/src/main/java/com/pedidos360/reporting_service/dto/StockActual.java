package com.pedidos360.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Último stock conocido de un producto, según el tópico compactado
 * "stock.eventos". ActualizadoEn es el timestamp que envió catalog-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockActual {

    private Long productoId;
    private String nombre;
    private int stock;
    private int precio;
    private LocalDateTime actualizadoEn;
}
