package com.pedidos360.notifications_service.controller;

import com.pedidos360.notifications_service.admin.RabbitAdminService;
import com.pedidos360.notifications_service.dto.QueueRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class QueueControllerTests {

    private final RabbitAdminService admin = mock(RabbitAdminService.class);
    private final QueueController controller = new QueueController(admin);

    @Test
    void crearDevuelve201ConNombreYEstado() {
        var respuesta = controller.crear(new QueueRequest("notificaciones.queue"));

        assertEquals(201, respuesta.getStatusCode().value());
        assertEquals("CREADA", respuesta.getBody().get("estado"));
        assertEquals("notificaciones.queue", respuesta.getBody().get("nombre"));
        verify(admin).crearCola("notificaciones.queue");
    }

    @Test
    void eliminarDevuelve204() {
        var respuesta = controller.eliminar("notificaciones.queue");

        assertEquals(204, respuesta.getStatusCode().value());
        verify(admin).eliminarCola("notificaciones.queue");
    }
}
