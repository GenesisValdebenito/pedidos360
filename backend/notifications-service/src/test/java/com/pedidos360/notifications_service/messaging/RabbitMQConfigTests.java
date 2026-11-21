package com.pedidos360.notifications_service.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitMQConfigTests {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void colaPrincipalEstaSuscribeAlDlx() {
        Queue cola = config.notificacionesQueue();

        assertEquals(RabbitMQConfig.NOTIFICACIONES_DLX,
                cola.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConfig.DLQ_ROUTING_KEY,
                cola.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void existeDlxYDlqDeclaradas() {
        assertNotNull(config.pedidosDlx());
        assertNotNull(config.notificacionesDlq());
        assertNotNull(config.notificacionesDlqBinding(config.notificacionesDlq(), config.pedidosDlx()));
    }
}
