package com.pedidos360.reporting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * KPIs calculados en memoria por el grupo reporteria-group.
 *
 * pedidosCreados: cantidad de eventos con estado CREADO.
 * unidadesSolicitadas: suma de cantidades de esos pedidos.
 * pedidosPorEstado: cuántos eventos llegaron de cada estado del flujo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiResponse {

    private long pedidosCreados;
    private long unidadesSolicitadas;
    private long pedidosCancelados;
    private long pedidosEntregados;
    private Map<String, Long> pedidosPorEstado;
    private double tasaCancelacionPorcentaje;
    private double promedioUnidadesPorPedido;
    private LocalDateTime ultimaActualizacion;
}
