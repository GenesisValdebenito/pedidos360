package com.pedidos360.reporting_service.service;

import com.pedidos360.reporting_service.dto.KpiResponse;
import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calcula KPIs en memoria a partir de los eventos de pedidos.
 *
 * Este servicio lo alimenta el consumidor del grupo "reporteria-group".
 * Los contadores viven solo mientras el proceso está arriba: al reiniciar
 * se reconstruyen al volver a leer el tópico (auto-offset-reset=earliest).
 *
 * Los métodos son synchronized porque Kafka podría entregar mensajes en
 * varios hilos si se aumenta la concurrencia del contenedor. Con el volumen
 * del proyecto es la forma más simple de evitar condiciones de carrera.
 */
@Service
public class KpiService {

    private final Map<String, Long> pedidosPorEstado = new TreeMap<>();
    private long pedidosCreados;
    private long unidadesSolicitadas;
    private LocalDateTime ultimaActualizacion;

    /** Procesa un evento de pedido y actualiza los contadores. */
    public synchronized void registrar(PedidoEventoKafka evento) {
        pedidosPorEstado.merge(evento.getEstado(), 1L, Long::sum);

        if ("CREADO".equals(evento.getEstado())) {
            pedidosCreados++;
            unidadesSolicitadas += evento.getCantidad();
        }

        ultimaActualizacion = LocalDateTime.now();
    }

    /** Devuelve una foto de los KPIs en el momento de la consulta. */
    public synchronized KpiResponse obtener() {
        long cancelados = pedidosPorEstado.getOrDefault("CANCELADO", 0L);
        long entregados = pedidosPorEstado.getOrDefault("ENTREGADO", 0L);

        double tasaCancelacion = pedidosCreados == 0
                ? 0.0
                : (cancelados * 100.0) / pedidosCreados;

        double promedioUnidades = pedidosCreados == 0
                ? 0.0
                : (double) unidadesSolicitadas / pedidosCreados;

        return new KpiResponse(
                pedidosCreados,
                unidadesSolicitadas,
                cancelados,
                entregados,
                new TreeMap<>(pedidosPorEstado),
                redondear(tasaCancelacion),
                redondear(promedioUnidades),
                ultimaActualizacion);
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
