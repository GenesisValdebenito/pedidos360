package com.pedidos360.orders_service.service;

import com.pedidos360.orders_service.model.Order;
import com.pedidos360.orders_service.messaging.PedidoEvento;
import com.pedidos360.orders_service.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderService orderService;
    private Order pedido;

    @BeforeEach
    void configurar() {
        orderService = new OrderService(orderRepository, restTemplate, "secreto-de-prueba", eventPublisher);
        pedido = new Order(1L, "cliente@ejemplo.com", "CREADO", 20L, 2);
    }

    @Test
    void aceptarPedidoDescuentaStockUnaSolaVez() {
        when(orderRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(pedido));
        when(orderRepository.save(pedido)).thenReturn(pedido);

        Order actualizado = orderService.cambiarEstado(1L, "ACEPTADO");

        assertEquals("ACEPTADO", actualizado.getEstado());
        verify(eventPublisher).publishEvent(any(PedidoEvento.class));
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://localhost:8082/api/catalog/products/20/descontar-stock?cantidad=2"),
                eq(HttpMethod.PUT),
                requestCaptor.capture(),
                eq(Void.class));
        assertEquals("secreto-de-prueba",
                requestCaptor.getValue().getHeaders().getFirst("X-Internal-Service-Token"));

        assertThrows(RuntimeException.class, () -> orderService.cambiarEstado(1L, "ACEPTADO"));
        verify(restTemplate, times(1)).exchange(
                anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void noPermiteSaltarDirectamenteDesdeCreadoAHastaEntregado() {
        when(orderRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(pedido));

        assertThrows(RuntimeException.class, () -> orderService.cambiarEstado(1L, "ENTREGADO"));

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void permiteCancelarUnPedidoSoloMientrasEstaCreado() {
        when(orderRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(pedido));
        when(orderRepository.save(pedido)).thenReturn(pedido);

        assertEquals("CANCELADO", orderService.cambiarEstado(1L, "CANCELADO").getEstado());

        assertThrows(RuntimeException.class, () -> orderService.cambiarEstado(1L, "ACEPTADO"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void alCrearPedidoPublicaEventoDeCreacion() {
        when(orderRepository.save(pedido)).thenReturn(pedido);

        Order creado = orderService.crear(pedido, "cliente@ejemplo.com");

        assertEquals("CREADO", creado.getEstado());
        org.mockito.ArgumentCaptor<Object> eventoCaptor =
                org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventoCaptor.capture());
        assertEquals(PedidoEvento.class, eventoCaptor.getValue().getClass());
        assertEquals("CREADO", ((PedidoEvento) eventoCaptor.getValue()).getEstado());
    }
}
