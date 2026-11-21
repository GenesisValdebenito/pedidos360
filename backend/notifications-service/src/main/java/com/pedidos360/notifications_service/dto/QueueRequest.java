package com.pedidos360.notifications_service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para crear una cola de RabbitMQ.
 *
 * El nombre es obligatorio: se valida con Bean Validation (@NotBlank)
 * antes de llegar al servicio, y el controlador responde 400 si falta.
 */
public class QueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String name;

    public QueueRequest() {
    }

    public QueueRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
