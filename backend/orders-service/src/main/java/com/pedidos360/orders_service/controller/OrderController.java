package com.pedidos360.orders_service.controller;

import com.pedidos360.orders_service.model.Order;
import com.pedidos360.orders_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller de pedidos — recibe las peticiones HTTP, llama al Service
 * y devuelve la respuesta con el código HTTP correcto.
 *
 * @RestController: esta clase maneja peticiones HTTP y devuelve JSON.
 * @RequestMapping: URL base para todos los endpoints de este controller.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/orders
     * Crea un pedido nuevo.
     * Cualquier usuario autenticado puede crear pedidos.
     *
     * El "cliente" del pedido se extrae del JWT automáticamente (preferred_username),
     * el usuario NO lo envía en el JSON.
     *
     * Responde 201 Created con el pedido creado.
     */
    @PostMapping
    public ResponseEntity<Order> crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Order pedido) {

        // Extraemos el username del token JWT (el campo "preferred_username" de Microsoft Entra)
        String cliente = jwt.getClaimAsString("preferred_username");

        Order creado = orderService.crear(pedido, cliente);
        return ResponseEntity.status(201).body(creado);
    }

    /**
     * GET /api/orders
     * Retorna la lista de pedidos.
     * - Admin u Operador → ven TODOS los pedidos del sistema.
     * - Cliente → ve solo sus propios pedidos.
     *
     * Responde 200 con la lista.
     */
    @GetMapping
    public ResponseEntity<List<Order>> listar(@AuthenticationPrincipal Jwt jwt) {
        // Leemos el username y los roles del token JWT
        String cliente = jwt.getClaimAsString("preferred_username");
        List<String> roles = jwt.getClaimAsStringList("roles");

        // Verificamos si tiene rol de Admin u Operador
        boolean esAdminOperador = roles != null &&
                (roles.contains("Admin") || roles.contains("Operador"));

        return ResponseEntity.ok(orderService.listar(cliente, esAdminOperador));
    }

    /**
     * GET /api/orders/{id}
     * Busca un pedido por su ID.
     * 200 si existe, 404 si no existe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> buscarPorId(@PathVariable Long id) {
        Order pedido = orderService.buscarPorId(id);
        if (pedido == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pedido);
    }

    /**
     * PUT /api/orders/{id}/status
     * Cambia el estado de un pedido.
     * Solo Admin u Operador pueden hacerlo. Si es Cliente, responde 403.
     *
     * El nuevo estado llega en el body como JSON: {"estado": "ACEPTADO"}
     * Usamos Map<String, String> para leer ese JSON simple sin crear una clase extra.
     *
     * Códigos de respuesta:
     * - 200: estado cambiado correctamente
     * - 400: regla de negocio no cumplida (ej. despachar sin aceptar)
     * - 403: el usuario no tiene permiso
     * - 404: el pedido no existe
     *
     * Nota sobre el header Authorization:
     * Lo leemos manualmente del request para poder reenviárselo a catalog-service
     * cuando el nuevo estado es "ACEPTADO" y hay que descontar stock.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> cambiarEstado(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authorizationHeader) {

        // Verificamos que el usuario sea Admin u Operador
        List<String> roles = jwt.getClaimAsStringList("roles");
        boolean esAdminOperador = roles != null &&
                (roles.contains("Admin") || roles.contains("Operador"));

        if (!esAdminOperador) {
            return ResponseEntity.status(403).build();
        }

        // Extraemos el nuevo estado del body JSON
        String nuevoEstado = body.get("estado");

        try {
            Order pedido = orderService.cambiarEstado(id, nuevoEstado, authorizationHeader);

            // Si el pedido no existía, el service retorna null
            if (pedido == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(pedido);

        } catch (RuntimeException e) {
            // Capturamos cualquier excepción de regla de negocio
            // (ej: "No se puede despachar sin antes aceptar el pedido"
            //  o stock insuficiente desde catalog-service)
            // y respondemos con 400 Bad Request
            return ResponseEntity.badRequest().build();
        }
    }
}
