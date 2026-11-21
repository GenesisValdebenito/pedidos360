package com.pedidos360.notifications_service.controller;

import com.pedidos360.notifications_service.admin.RabbitAdminService;
import com.pedidos360.notifications_service.dto.QueueRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de administración de colas RabbitMQ.
 *
 * POST   /api/queues        → crea una cola durable
 * DELETE /api/queues/{name} → elimina una cola
 *
 * La validación del DTO (QueueRequest) es automática gracias a @Valid:
 * si falta "name", Spring responde 400 Bad Request.
 *
 * NOTA DE SEGURIDAD: este endpoint es de administración. En producción debe
 * quedar restringido a la red interna / VPN y protegido (rol Admin o token
 * interno), ya que hoy no tiene autenticación propia.
 */
@RestController
@RequestMapping("/api/queues")
public class QueueController {

    private final RabbitAdminService rabbitAdminService;

    public QueueController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    /**
     * Crea una cola durable.
     * 201 Created con el nombre y el estado.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> crear(@Valid @RequestBody QueueRequest request) {
        rabbitAdminService.crearCola(request.getName());
        return ResponseEntity.status(201).body(
                Map.of("nombre", request.getName(), "estado", "CREADA"));
    }

    /**
     * Elimina una cola por nombre.
     * 204 No Content.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> eliminar(@PathVariable String name) {
        rabbitAdminService.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }
}
