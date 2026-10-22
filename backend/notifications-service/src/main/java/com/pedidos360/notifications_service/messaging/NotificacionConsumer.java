package com.pedidos360.notifications_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.NOTIFICACIONES_QUEUE)
    public void recibir(String mensaje) {
        logger.info("[NOTIFICACION] Evento recibido: {}", mensaje);
    }
}
