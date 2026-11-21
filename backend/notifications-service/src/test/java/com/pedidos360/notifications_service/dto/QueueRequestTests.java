package com.pedidos360.notifications_service.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nameEsObligatorio() {
        assertFalse(validator.validate(new QueueRequest(" ")).isEmpty());
        assertFalse(validator.validate(new QueueRequest(null)).isEmpty());
    }

    @Test
    void nameValidoNoGeneraViolaciones() {
        assertTrue(validator.validate(new QueueRequest("notificaciones.queue")).isEmpty());
    }
}
