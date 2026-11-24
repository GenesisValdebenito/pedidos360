package com.pedidos360.kafka_admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Detalle de una partición de un tópico.
 * líder y las réplicas son IDs de broker (0, 1, 2...).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticionInfo {

    private int particion;
    private Integer lider;
    private List<Integer> replicas;
    private List<Integer> isr;
}
