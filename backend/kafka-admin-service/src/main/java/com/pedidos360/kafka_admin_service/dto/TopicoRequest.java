package com.pedidos360.kafka_admin_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos para crear un tópico.
 *
 * Las validaciones se ejecutan automáticamente con @Valid en el controller.
 * Los valores por defecto coinciden con el diseño de Pedidos360: 3 particiones,
 * 3 réplicas, retención de 7 días y cleanup.policy=delete.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicoRequest {

    @NotBlank(message = "El nombre del tópico es obligatorio")
    private String nombre;

    @Min(value = 1, message = "Debe haber al menos 1 partición")
    private int particiones = 3;

    @Min(value = 1, message = "Debe haber al menos 1 réplica")
    private short replicas = 3;

    /** Retención en milisegundos. 604800000 = 7 días. */
    private Long retentionMs = 604800000L;

    @Pattern(regexp = "delete|compact|compact,delete",
            message = "cleanup.policy debe ser delete, compact o compact,delete")
    private String cleanupPolicy = "delete";

    @Min(value = 1, message = "min.insync.replicas debe ser al menos 1")
    private int minInSyncReplicas = 2;
}
