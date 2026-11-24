package com.pedidos360.kafka_admin_service.controller;

import com.pedidos360.kafka_admin_service.dto.TopicoRequest;
import com.pedidos360.kafka_admin_service.service.KafkaAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KafkaAdminControllerTests {

    private final KafkaAdminService service = mock(KafkaAdminService.class);
    private final KafkaAdminController controller = new KafkaAdminController(service);

    private Jwt jwtConRoles(List<String> roles) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsStringList("roles")).thenReturn(roles);
        return jwt;
    }

    @Test
    void sinRolAdminDevuelve403() {
        var respuesta = controller.listar(jwtConRoles(List.of("Cliente")));

        assertEquals(403, respuesta.getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test
    void adminListaTopicos() {
        when(service.listarTopicos()).thenReturn(List.of("pedidos.eventos"));

        var respuesta = controller.listar(jwtConRoles(List.of("Admin")));

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(List.of("pedidos.eventos"), respuesta.getBody());
    }

    @Test
    void adminCreaTopico() {
        var request = new TopicoRequest(
                "nuevo.topico", 3, (short) 3, 1000L, "delete", 2);

        var respuesta = controller.crear(jwtConRoles(List.of("Admin")), request);

        assertEquals(201, respuesta.getStatusCode().value());
        verify(service).crearTopico(any());
    }
}
