package com.pedidos360.orders_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrdersTestController {

    @GetMapping("/publico")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of("mensaje", "Endpoint público operativo"));
    }

    @GetMapping("/pedidos")
    public ResponseEntity<Map<String, Object>> getPedidos(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "desconocido";

        List<Map<String, Object>> pedidosEjemplo = List.of(
            Map.of("id", 101, "producto", "Laptop Pro 16", "cantidad", 1, "estado", "CONFIRMADO"),
            Map.of("id", 102, "producto", "Mouse Inalámbrico", "cantidad", 2, "estado", "EN_PREPARACION")
        );

        return ResponseEntity.ok(Map.of(
            "servicio", "orders-service",
            "usuario", username,
            "pedidos", pedidosEjemplo
        ));
    }
}
