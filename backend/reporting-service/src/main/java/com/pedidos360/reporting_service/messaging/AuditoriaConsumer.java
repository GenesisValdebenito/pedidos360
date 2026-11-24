package com.pedidos360.reporting_service.messaging;

import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import com.pedidos360.reporting_service.dto.StockEventoKafka;
import com.pedidos360.reporting_service.service.AuditoriaService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del grupo "auditoria-group".
 *
 * Escucha los dos tópicos:
 * - pedidos.eventos: arma la línea de tiempo de cambios de pedido.
 * - stock.eventos: guarda el último stock conocido de cada producto.
 *
 * Ambos métodos comparten groupId. Es válido: un grupo puede tener
 * consumidores suscritos a tópicos distintos, y Kafka lleva los offsets
 * por grupo y por partición.
 */
@Component
public class AuditoriaConsumer {

    private final AuditoriaService auditoriaService;

    public AuditoriaConsumer(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @KafkaListener(
            topics = "pedidos.eventos",
            groupId = "auditoria-group",
            containerFactory = "pedidoListenerContainerFactory")
    public void consumirPedido(PedidoEventoKafka evento) {
        auditoriaService.registrarPedido(evento);
    }

    @KafkaListener(
            topics = "stock.eventos",
            groupId = "auditoria-group",
            containerFactory = "stockListenerContainerFactory")
    public void consumirStock(StockEventoKafka evento) {
        auditoriaService.registrarStock(evento);
    }
}
