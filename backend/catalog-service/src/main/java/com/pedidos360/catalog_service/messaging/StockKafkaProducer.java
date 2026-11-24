package com.pedidos360.catalog_service.messaging;

import com.pedidos360.catalog_service.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Publica en Kafka el stock actual de un producto.
 *
 * Se llama después de guardar en la base de datos, desde ProductService.
 * La clave es productoId: al ser un tópico compactado, Kafka mantiene el
 * último evento de cada producto y descarta los anteriores.
 */
@Service
public class StockKafkaProducer {

    public static final String TOPICO = "stock.eventos";

    private static final Logger logger = LoggerFactory.getLogger(StockKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StockKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(Product producto) {
        StockEventoKafka evento = new StockEventoKafka(
                producto.getId(),
                producto.getName(),
                producto.getStock(),
                producto.getPrice(),
                LocalDateTime.now());

        kafkaTemplate.send(TOPICO, String.valueOf(producto.getId()), evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        logger.error("No fue posible publicar el stock del producto {} en Kafka",
                                producto.getId(), error);
                    }
                });
    }
}
