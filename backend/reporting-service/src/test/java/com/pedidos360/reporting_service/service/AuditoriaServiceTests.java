package com.pedidos360.reporting_service.service;

import com.pedidos360.reporting_service.dto.EventoAuditoria;
import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import com.pedidos360.reporting_service.dto.StockActual;
import com.pedidos360.reporting_service.dto.StockEventoKafka;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditoriaServiceTests {

    private AuditoriaService auditoriaService;

    @BeforeEach
    void configurar() {
        auditoriaService = new AuditoriaService();
    }

    private PedidoEventoKafka pedido(Long id, String estado) {
        return new PedidoEventoKafka(
                id, estado, "cliente@ejemplo.com", 20L, 1, LocalDateTime.now());
    }

    @Test
    void laLineaDeTiempoMuestraPrimeroElEventoMasReciente() {
        auditoriaService.registrarPedido(pedido(1L, "CREADO"));
        auditoriaService.registrarPedido(pedido(2L, "CREADO"));
        auditoriaService.registrarPedido(pedido(3L, "ACEPTADO"));

        List<EventoAuditoria> eventos = auditoriaService.obtenerLineaTiempo(2);

        assertEquals(2, eventos.size());
        assertEquals(3L, eventos.get(0).getPedidoId());
        assertEquals(2L, eventos.get(1).getPedidoId());
    }

    @Test
    void elStockSeReemplazaConElUltimoEventoDelProducto() {
        auditoriaService.registrarStock(new StockEventoKafka(
                20L, "Teclado", 7, 1500, LocalDateTime.now()));
        auditoriaService.registrarStock(new StockEventoKafka(
                20L, "Teclado", 5, 1500, LocalDateTime.now()));

        StockActual stock = auditoriaService.buscarStock(20L);

        assertEquals(5, stock.getStock());
        assertEquals(1, auditoriaService.obtenerStock().size());
        assertNull(auditoriaService.buscarStock(99L));
    }

    @Test
    void laLineaDeTiempoSeLimitaA100Eventos() {
        for (int i = 1; i <= 105; i++) {
            auditoriaService.registrarPedido(pedido((long) i, "CREADO"));
        }

        assertEquals(100, auditoriaService.obtenerLineaTiempo(100).size());
        assertEquals(105L, auditoriaService.obtenerLineaTiempo(100).get(0).getPedidoId());
    }
}
