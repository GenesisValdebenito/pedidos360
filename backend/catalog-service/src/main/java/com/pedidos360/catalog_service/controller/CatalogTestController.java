package com.pedidos360.catalog_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogTestController {

    @GetMapping("/publico")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of("mensaje", "Endpoint público operativo"));
    }

    @GetMapping("/productos")
    public ResponseEntity<Map<String, Object>> getProductos(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "desconocido";

        List<Map<String, Object>> productosEjemplo = List.of(
            Map.of("id", 201, "nombre", "Laptop Pro 16", "precio", 1200.00, "stock", 15),
            Map.of("id", 202, "nombre", "Mouse Inalámbrico", "precio", 25.50, "stock", 100)
        );

        return ResponseEntity.ok(Map.of(
            "servicio", "catalog-service",
            "usuario", username,
            "productos", productosEjemplo
        ));
    }
}
