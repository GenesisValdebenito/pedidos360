package com.pedidos360.notifications_service.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola de notificaciones.
 *
 * ACK MANUAL: el contenedor no confirma nada automáticamente. El mensaje
 * queda en estado "Unacked" hasta que este método ejecuta:
 *   - basicAck  → procesamiento correcto, se descarta del broker.
 *   - basicNack(requeue=false) → fallo, el mensaje NO vuelve a la cola y
 *     el exchange de letras (DLX) lo enruta a la DLQ.
 *
 * Este es el mecanismo que permite demostrar confiabilidad: un mensaje que
 * no se pudo procesar termina en la DLQ en lugar de perderse.
 */
@Component
public class NotificacionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.NOTIFICACIONES_QUEUE)
    public void recibir(String mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            procesar(mensaje);
            // ACK manual: confirmamos solo después de procesar el mensaje.
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            logger.error("[NOTIFICACION] Error procesando el mensaje; se envía a la DLQ", e);
            try {
                // requeue=false → el mensaje no se reencola; queda en la DLQ.
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                logger.error("[NOTIFICACION] No fue posible hacer NACK del mensaje", ex);
            }
        }
    }

    /**
     * Lógica de procesamiento del evento. Se separa para poder simular
     * fallos en las pruebas.
     */
    protected void procesar(String mensaje) {
        logger.info("[NOTIFICACION] Evento recibido: {}", mensaje);
    }
}
