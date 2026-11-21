package com.pedidos360.notifications_service.admin;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

/**
 * Servicio de administración de RabbitMQ.
 *
 * Encapsula toda la lógica de creación, consulta y eliminación de colas
 * para que los controladores no conozcan los detalles de RabbitMQ.
 * Los controladores llaman a métodos de alto nivel (crearCola, eliminarCola).
 *
 * RabbitAdmin es el componente de Spring AMQP que ejecuta operaciones de
 * administración (declarar/borrar exchanges, colas y bindings) sobre la
 * conexión configurada. Spring Boot lo registra como bean automáticamente.
 */
@Service
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    /**
     * Crea una cola durable. Si ya existe con la misma definición, no falla
     * (idempotente).
     *
     * @param nombre nombre de la cola (no vacío)
     */
    public void crearCola(String nombre) {
        validarNombre(nombre);
        if (!existeCola(nombre)) {
            Queue cola = QueueBuilder.durable(nombre).build();
            rabbitAdmin.declareQueue(cola);
        }
    }

    /**
     * Elimina una cola por nombre.
     *
     * @param nombre nombre de la cola a eliminar
     */
    public void eliminarCola(String nombre) {
        validarNombre(nombre);
        rabbitAdmin.deleteQueue(nombre);
    }

    /**
     * Indica si una cola existe en el broker.
     * getQueueProperties devuelve null cuando la cola no existe.
     */
    public boolean existeCola(String nombre) {
        return rabbitAdmin.getQueueProperties(nombre) != null;
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la cola no puede estar vacío");
        }
    }
}
