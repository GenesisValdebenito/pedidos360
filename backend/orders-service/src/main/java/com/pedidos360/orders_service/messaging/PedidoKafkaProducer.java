package com.pedidos360.orders_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica los eventos de pedido en Kafka.
 *
 * Se mantiene el mismo patrón que PedidoEventoProducer de RabbitMQ:
 * @TransactionalEventListener con fase AFTER_COMMIT. Así el evento solo
 * viaja a Kafka cuando la transacción de base de datos terminó bien; si el
 * pedido no se guardó, no se publica nada.
 *
 * La clave del mensaje es el pedidoId, para que todos los eventos de un mismo
 * pedido caigan en la misma partición y se lean en orden.
 */
@Service
public class PedidoKafkaProducer {

    public static final String TOPICO = "pedidos.eventos";

    private static final Logger logger = LoggerFactory.getLogger(PedidoKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PedidoKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(PedidoEventoKafka evento) {
        kafkaTemplate.send(TOPICO, String.valueOf(evento.getPedidoId()), evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        logger.error("No fue posible publicar el pedido {} en Kafka",
                                evento.getPedidoId(), error);
                    }
                });
    }
}
