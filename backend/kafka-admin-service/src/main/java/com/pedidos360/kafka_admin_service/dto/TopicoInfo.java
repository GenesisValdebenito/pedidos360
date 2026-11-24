package com.pedidos360.kafka_admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Respuesta con el detalle de un tópico: particiones, réplicas, ISR y
 * configuraciones efectivas (retention.ms, cleanup.policy, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicoInfo {

    private String nombre;
    private int particiones;
    private int replicas;
    private List<ParticionInfo> detalleParticiones;
    private Map<String, String> configuraciones;
}
