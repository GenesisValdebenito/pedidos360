package com.pedidos360.orders_service.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedidoKafkaProducerTests {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private final PedidoKafkaProducer producer = new PedidoKafkaProducer(kafkaTemplate);

    @Test
    void publicaEnElTopicoConElPedidoIdComoClave() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        PedidoEventoKafka evento = new PedidoEventoKafka(
                1L, "CREADO", "cliente@ejemplo.com", 20L, 2, LocalDateTime.now());

        producer.publicar(evento);

        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("pedidos.eventos"),
                org.mockito.ArgumentMatchers.eq("1"),
                eventoCaptor.capture());

        assertEquals(evento, eventoCaptor.getValue());
    }
}
