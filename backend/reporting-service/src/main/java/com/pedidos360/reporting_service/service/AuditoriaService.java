package com.pedidos360.reporting_service.service;

import com.pedidos360.reporting_service.dto.EventoAuditoria;
import com.pedidos360.reporting_service.dto.PedidoEventoKafka;
import com.pedidos360.reporting_service.dto.StockActual;
import com.pedidos360.reporting_service.dto.StockEventoKafka;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Guarda en memoria la línea de tiempo de pedidos y el último stock por
 * producto. Lo alimenta el consumidor del grupo "auditoria-group".
 *
 * La línea de tiempo se limita a los últimos 100 eventos para no crecer sin
 * control. El stock queda en un mapa indexado por productoId; como el tópico
 * es compactado, cada mensaje nuevo reemplaza el estado anterior.
 *
 * Los métodos son synchronized por la misma razón que en KpiService: el
 * contenedor de Kafka podría usar varios hilos.
 */
@Service
public class AuditoriaService {

    private static final int MAX_EVENTOS = 100;

    private final Deque<EventoAuditoria> lineaTiempo = new ArrayDeque<>();
    private final Map<Long, StockActual> stockPorProducto = new TreeMap<>();

    /** Agrega un evento de pedido al inicio de la línea de tiempo. */
    public synchronized void registrarPedido(PedidoEventoKafka evento) {
        lineaTiempo.addFirst(new EventoAuditoria(
                evento.getPedidoId(),
                evento.getEstado(),
                evento.getCliente(),
                evento.getProductoId(),
                evento.getCantidad(),
                evento.getTimestamp(),
                LocalDateTime.now()));

        while (lineaTiempo.size() > MAX_EVENTOS) {
            lineaTiempo.removeLast();
        }
    }

    /** Actualiza el último stock conocido del producto. */
    public synchronized void registrarStock(StockEventoKafka evento) {
        stockPorProducto.put(evento.getProductoId(), new StockActual(
                evento.getProductoId(),
                evento.getNombre(),
                evento.getStock(),
                evento.getPrecio(),
                evento.getTimestamp()));
    }

    /** Devuelve los últimos eventos, del más reciente al más antiguo. */
    public synchronized List<EventoAuditoria> obtenerLineaTiempo(int limite) {
        return lineaTiempo.stream()
                .limit(limite)
                .toList();
    }

    /** Devuelve el stock de todos los productos conocidos. */
    public synchronized List<StockActual> obtenerStock() {
        return new ArrayList<>(stockPorProducto.values());
    }

    /** Busca el stock de un producto; null si todavía no llegó ningún evento. */
    public synchronized StockActual buscarStock(Long productoId) {
        return stockPorProducto.get(productoId);
    }
}
