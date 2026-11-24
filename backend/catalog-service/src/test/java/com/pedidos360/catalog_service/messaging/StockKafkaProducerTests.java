package com.pedidos360.catalog_service.messaging;

import com.pedidos360.catalog_service.model.Product;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockKafkaProducerTests {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private final StockKafkaProducer producer = new StockKafkaProducer(kafkaTemplate);

    @Test
    void publicaElStockConElProductoIdComoClave() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Product producto = new Product(20L, "Teclado", 1500, 7);

        producer.publicar(producto);

        ArgumentCaptor<Object> eventoCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(
                eq("stock.eventos"),
                eq("20"),
                eventoCaptor.capture());

        StockEventoKafka evento = (StockEventoKafka) eventoCaptor.getValue();
        assertEquals(20L, evento.getProductoId());
        assertEquals("Teclado", evento.getNombre());
        assertEquals(7, evento.getStock());
        assertEquals(1500, evento.getPrecio());
    }
}
