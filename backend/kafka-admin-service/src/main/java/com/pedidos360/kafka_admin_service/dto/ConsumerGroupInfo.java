package com.pedidos360.kafka_admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Estado de un consumer group: nombre, estado, cantidad de miembros y
 * offsets/lag por partición.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupInfo {

    private String grupo;
    private String estado;
    private int miembros;
    private List<OffsetParticion> offsets;
}
