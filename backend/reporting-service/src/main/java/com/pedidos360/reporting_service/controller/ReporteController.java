package com.pedidos360.reporting_service.controller;

import com.pedidos360.reporting_service.dto.EventoAuditoria;
import com.pedidos360.reporting_service.dto.KpiResponse;
import com.pedidos360.reporting_service.dto.StockActual;
import com.pedidos360.reporting_service.service.AuditoriaService;
import com.pedidos360.reporting_service.service.KpiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de reportes. Solo Admin y Operador.
 *
 * Spring Security ya validó firma, issuer, audience y scope Pedidos.Read.
 * Aquí se revisa el claim "roles" para decidir si el usuario puede ver
 * los reportes, igual que en el resto de Pedidos360.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final KpiService kpiService;
    private final AuditoriaService auditoriaService;

    public ReporteController(KpiService kpiService, AuditoriaService auditoriaService) {
        this.kpiService = kpiService;
        this.auditoriaService = auditoriaService;
    }

    /** GET /api/reportes/kpis */
    @GetMapping("/kpis")
    public ResponseEntity<KpiResponse> kpis(@AuthenticationPrincipal Jwt jwt) {
        if (!puedeVerReportes(jwt)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(kpiService.obtener());
    }

    /**
     * GET /api/reportes/auditoria?limite=50
     * Devuelve los últimos eventos, del más reciente al más antiguo.
     */
    @GetMapping("/auditoria")
    public ResponseEntity<List<EventoAuditoria>> auditoria(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limite) {

        if (!puedeVerReportes(jwt)) {
            return ResponseEntity.status(403).build();
        }

        int limiteSeguro = Math.max(1, Math.min(limite, 100));
        return ResponseEntity.ok(auditoriaService.obtenerLineaTiempo(limiteSeguro));
    }

    /** GET /api/reportes/stock */
    @GetMapping("/stock")
    public ResponseEntity<List<StockActual>> stock(@AuthenticationPrincipal Jwt jwt) {
        if (!puedeVerReportes(jwt)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditoriaService.obtenerStock());
    }

    /** GET /api/reportes/stock/{productoId} */
    @GetMapping("/stock/{productoId}")
    public ResponseEntity<StockActual> stockProducto(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productoId) {

        if (!puedeVerReportes(jwt)) {
            return ResponseEntity.status(403).build();
        }

        StockActual stock = auditoriaService.buscarStock(productoId);
        if (stock == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stock);
    }

    private boolean puedeVerReportes(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && (roles.contains("Admin") || roles.contains("Operador"));
    }
}
