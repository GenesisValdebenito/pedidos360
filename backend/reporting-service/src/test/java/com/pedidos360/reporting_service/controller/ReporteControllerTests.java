package com.pedidos360.reporting_service.controller;

import com.pedidos360.reporting_service.dto.KpiResponse;
import com.pedidos360.reporting_service.dto.StockActual;
import com.pedidos360.reporting_service.service.AuditoriaService;
import com.pedidos360.reporting_service.service.KpiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReporteControllerTests {

    private KpiService kpiService;
    private AuditoriaService auditoriaService;
    private ReporteController controller;

    @BeforeEach
    void configurar() {
        kpiService = mock(KpiService.class);
        auditoriaService = mock(AuditoriaService.class);
        controller = new ReporteController(kpiService, auditoriaService);
    }

    private Jwt jwtConRoles(List<String> roles) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsStringList("roles")).thenReturn(roles);
        return jwt;
    }

    @Test
    void unClienteNoPuedeVerReportes() {
        var respuesta = controller.kpis(jwtConRoles(List.of("Cliente")));

        assertEquals(403, respuesta.getStatusCode().value());
        verifyNoInteractions(kpiService);
    }

    @Test
    void unOperadorPuedeVerKpis() {
        KpiResponse kpis = new KpiResponse(
                1, 2, 0, 0, Map.of("CREADO", 1L), 0.0, 2.0, null);
        when(kpiService.obtener()).thenReturn(kpis);

        var respuesta = controller.kpis(jwtConRoles(List.of("Operador")));

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, respuesta.getBody().getPedidosCreados());
    }

    @Test
    void unAdminPuedeVerStock() {
        when(auditoriaService.obtenerStock()).thenReturn(List.of(
                new StockActual(20L, "Teclado", 5, 1500, null)));

        var respuesta = controller.stock(jwtConRoles(List.of("Admin")));

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, respuesta.getBody().size());
    }

    @Test
    void stockInexistenteDevuelve404() {
        when(auditoriaService.buscarStock(99L)).thenReturn(null);

        var respuesta = controller.stockProducto(jwtConRoles(List.of("Admin")), 99L);

        assertEquals(404, respuesta.getStatusCode().value());
    }

    @Test
    void laAuditoriaLimitaElMaximoA100() {
        when(auditoriaService.obtenerLineaTiempo(100)).thenReturn(List.of());

        var respuesta = controller.auditoria(jwtConRoles(List.of("Admin")), 500);

        assertEquals(200, respuesta.getStatusCode().value());
        verify(auditoriaService).obtenerLineaTiempo(100);
    }
}
