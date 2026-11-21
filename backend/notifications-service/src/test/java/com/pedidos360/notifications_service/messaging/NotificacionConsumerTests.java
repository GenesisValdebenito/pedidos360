package com.pedidos360.notifications_service.messaging;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionConsumerTests {

    @Mock
    private Channel channel;

    @Test
    void confirmaConAckCuandoElProcesamientoEsExitoso() throws Exception {
        NotificacionConsumer consumer = new NotificacionConsumer();

        consumer.recibir("pedido.creado", channel, 1L);

        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void enviaALaDlqConNackCuandoElProcesamientoFalla() throws Exception {
        // Spy para simular un fallo en la lógica de procesamiento.
        NotificacionConsumer consumer = spy(new NotificacionConsumer());
        doThrow(new RuntimeException("error al procesar"))
                .when(consumer).procesar("pedido.creado");

        consumer.recibir("pedido.creado", channel, 1L);

        // requeue=false: el mensaje no vuelve a la cola, va a la DLQ.
        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
