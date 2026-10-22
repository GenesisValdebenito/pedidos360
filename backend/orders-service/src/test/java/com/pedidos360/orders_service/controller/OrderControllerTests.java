package com.pedidos360.orders_service.controller;

import com.pedidos360.orders_service.model.Order;
import com.pedidos360.orders_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderControllerTests {

    private OrderService orderService;
    private OrderController orderController;

    @BeforeEach
    void configurar() {
        orderService = mock(OrderService.class);
        orderController = new OrderController(orderService);
    }

    @Test
    void clienteNoPuedeConsultarPedidoDeOtraPersona() {
        Order pedido = new Order(1L, "otra-persona@ejemplo.com", "CREADO", 20L, 1);
        when(orderService.buscarPorId(1L)).thenReturn(pedido);

        var respuesta = orderController.buscarPorId(jwt("cliente@ejemplo.com", List.of("Cliente")), 1L);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
    }

    @Test
    void clientePuedeConsultarSuPropioPedido() {
        Order pedido = new Order(1L, "cliente@ejemplo.com", "CREADO", 20L, 1);
        when(orderService.buscarPorId(1L)).thenReturn(pedido);

        var respuesta = orderController.buscarPorId(jwt("cliente@ejemplo.com", List.of("Cliente")), 1L);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }

    @Test
    void adminPuedeConsultarPedidosDeOtrosClientes() {
        Order pedido = new Order(1L, "otra-persona@ejemplo.com", "CREADO", 20L, 1);
        when(orderService.buscarPorId(1L)).thenReturn(pedido);

        var respuesta = orderController.buscarPorId(jwt("admin@ejemplo.com", List.of("Admin")), 1L);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }

    private Jwt jwt(String username, List<String> roles) {
        return Jwt.withTokenValue("token-prueba")
                .header("alg", "none")
                .claim("preferred_username", username)
                .claim("roles", roles)
                .build();
    }
}
