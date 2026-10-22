package com.pedidos360.orders_service.messaging;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class PedidoEventoProducer {

    private static final Logger logger = LoggerFactory.getLogger(PedidoEventoProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PedidoEventoProducer(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(PedidoEvento evento) {
        try {
            String mensaje = objectMapper.writeValueAsString(evento);
            String routingKey = RabbitMQConfig.routingKeyParaEstado(evento.getEstado());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    routingKey,
                    mensaje,
                    message -> {
                        message.getMessageProperties()
                                .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    });
        } catch (JacksonException e) {
            logger.error("No fue posible convertir el evento del pedido {} a JSON",
                    evento.getPedidoId(), e);
        } catch (AmqpException e) {
            logger.error("No fue posible publicar el evento del pedido {} en RabbitMQ; la notificación no quedó encolada",
                    evento.getPedidoId(), e);
        }
    }
}
