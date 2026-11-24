package com.pedidos360.kafka_admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Offset de una partición para un consumer group.
 *
 * offsetActual: última posición confirmada por el grupo.
 * offsetFinal: último offset escrito en la partición (log end offset).
 * lag: cuántos mensajes le faltan al grupo (offsetFinal - offsetActual).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffsetParticion {

    private String topico;
    private int particion;
    private long offsetActual;
    private long offsetFinal;
    private long lag;
}
