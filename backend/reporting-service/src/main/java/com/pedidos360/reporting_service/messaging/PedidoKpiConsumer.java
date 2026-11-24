package com.pedidos360.reporting_service.messaging;

import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import com.pedidos360.reporting_service.service.KpiService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del grupo "reporteria-group".
 *
 * Lee todos los eventos de pedidos y actualiza los KPIs. Al usar un grupo
 * distinto al de auditoría, este consumidor recibe su propia copia de cada
 * mensaje: los grupos de Kafka son independientes entre sí.
 */
@Component
public class PedidoKpiConsumer {

    private final KpiService kpiService;

    public PedidoKpiConsumer(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @KafkaListener(
            topics = "pedidos.eventos",
            groupId = "reporteria-group",
            containerFactory = "pedidoListenerContainerFactory")
    public void consumir(PedidoEventoKafka evento) {
        kpiService.registrar(evento);
    }
}
