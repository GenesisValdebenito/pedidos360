package com.pedidos360.kafka_admin_service.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos para modificar la configuración de un tópico existente.
 *
 * Ambos campos son opcionales, pero el servicio exige que al menos uno
 * venga informado (si no, responde 400).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicoConfigRequest {

    /** Nueva retención en milisegundos. -1 significa retención infinita. */
    private Long retentionMs;

    @Pattern(regexp = "delete|compact|compact,delete",
            message = "cleanup.policy debe ser delete, compact o compact,delete")
    private String cleanupPolicy;
}
