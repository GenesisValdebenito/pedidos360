package com.pedidos360.orders_service.messaging;

import java.time.LocalDateTime;

/**
 * Datos que se envían a RabbitMQ después de guardar un pedido.
 */
public class PedidoEvento {

    private Long pedidoId;
    private String estado;
    private String clienteEmail;
    private LocalDateTime timestamp;

    public PedidoEvento() {
    }

    public PedidoEvento(Long pedidoId, String estado, String clienteEmail, LocalDateTime timestamp) {
        this.pedidoId = pedidoId;
        this.estado = estado;
        this.clienteEmail = clienteEmail;
        this.timestamp = timestamp;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
