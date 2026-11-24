package com.pedidos360.reporting_service.service;

import com.pedidos360.reporting_service.dto.KpiResponse;
import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KpiServiceTests {

    private KpiService kpiService;

    @BeforeEach
    void configurar() {
        kpiService = new KpiService();
    }

    private PedidoEventoKafka evento(Long pedidoId, String estado, int cantidad) {
        return new PedidoEventoKafka(
                pedidoId, estado, "cliente@ejemplo.com", 20L, cantidad, LocalDateTime.now());
    }

    @Test
    void calculaKpisConVariosEstados() {
        kpiService.registrar(evento(1L, "CREADO", 2));
        kpiService.registrar(evento(2L, "CREADO", 3));
        kpiService.registrar(evento(1L, "ACEPTADO", 2));
        kpiService.registrar(evento(2L, "CANCELADO", 3));
        kpiService.registrar(evento(3L, "ENTREGADO", 1));

        KpiResponse kpis = kpiService.obtener();

        assertEquals(2, kpis.getPedidosCreados());
        assertEquals(5, kpis.getUnidadesSolicitadas());
        assertEquals(1, kpis.getPedidosCancelados());
        assertEquals(1, kpis.getPedidosEntregados());
        assertEquals(2, kpis.getPedidosPorEstado().get("CREADO"));
        assertEquals(1, kpis.getPedidosPorEstado().get("ACEPTADO"));
        assertEquals(50.0, kpis.getTasaCancelacionPorcentaje());
        assertEquals(2.5, kpis.getPromedioUnidadesPorPedido());
    }

    @Test
    void sinEventosDevuelveCeros() {
        KpiResponse kpis = kpiService.obtener();

        assertEquals(0, kpis.getPedidosCreados());
        assertEquals(0.0, kpis.getTasaCancelacionPorcentaje());
        assertEquals(0.0, kpis.getPromedioUnidadesPorPedido());
    }
}
