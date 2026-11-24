package com.pedidos360.kafka_admin_service.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicoRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nombreEsObligatorio() {
        TopicoRequest request = new TopicoRequest();
        request.setParticiones(3);
        request.setReplicas((short) 3);
        request.setCleanupPolicy("delete");
        request.setMinInSyncReplicas(2);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void cleanupPolicyInvalidoFalla() {
        TopicoRequest request = new TopicoRequest(
                "pedidos.eventos", 3, (short) 3, 604800000L, "otro", 2);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void requestValidoNoGeneraViolaciones() {
        TopicoRequest request = new TopicoRequest(
                "pedidos.eventos", 3, (short) 3, 604800000L, "compact", 2);

        assertTrue(validator.validate(request).isEmpty());
    }
}
