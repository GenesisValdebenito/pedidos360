package com.pedidos360.notifications_service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTests {

    @Mock
    private RabbitAdmin rabbitAdmin;

    private RabbitAdminService service;

    @BeforeEach
    void configurar() {
        service = new RabbitAdminService(rabbitAdmin);
    }

    @Test
    void creaColaDurableCuandoNoExiste() {
        when(rabbitAdmin.getQueueProperties("nueva.queue")).thenReturn(null);

        service.crearCola("nueva.queue");

        verify(rabbitAdmin).declareQueue(any(Queue.class));
    }

    @Test
    void noVuelveACrearColaSiYaExiste() {
        Properties props = new Properties();
        props.setProperty("x", "y");
        when(rabbitAdmin.getQueueProperties("ya.existe")).thenReturn(props);

        service.crearCola("ya.existe");

        verify(rabbitAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void eliminaCola() {
        service.eliminarCola("a.borrar");

        verify(rabbitAdmin).deleteQueue("a.borrar");
    }

    @Test
    void rechazaNombreVacioONulo() {
        assertThrows(IllegalArgumentException.class, () -> service.crearCola(" "));
        assertThrows(IllegalArgumentException.class, () -> service.eliminarCola(null));
    }
}
