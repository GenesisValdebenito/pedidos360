package com.pedidos360.kafka_admin_service.controller;

import com.pedidos360.kafka_admin_service.dto.ConsumerGroupInfo;
import com.pedidos360.kafka_admin_service.dto.TopicoConfigRequest;
import com.pedidos360.kafka_admin_service.dto.TopicoInfo;
import com.pedidos360.kafka_admin_service.dto.TopicoRequest;
import com.pedidos360.kafka_admin_service.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de administración de Kafka. Solo rol Admin.
 *
 * El JWT ya fue validado por Spring Security (firma, issuer, audience y
 * scope Pedidos.Read). Aquí se revisa el claim "roles" para exigir Admin,
 * igual que en el resto de Pedidos360.
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    /** POST /api/kafka/topicos */
    @PostMapping("/topicos")
    public ResponseEntity<Map<String, String>> crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TopicoRequest request) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(403).build();
        }

        kafkaAdminService.crearTopico(request);
        return ResponseEntity.status(201).body(Map.of(
                "mensaje", "Tópico creado",
                "nombre", request.getNombre()));
    }

    /** GET /api/kafka/topicos */
    @GetMapping("/topicos")
    public ResponseEntity<List<String>> listar(@AuthenticationPrincipal Jwt jwt) {
        if (!esAdmin(jwt)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(kafkaAdminService.listarTopicos());
    }

    /** GET /api/kafka/topicos/{nombre} */
    @GetMapping("/topicos/{nombre}")
    public ResponseEntity<TopicoInfo> describir(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String nombre) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(kafkaAdminService.describirTopico(nombre));
    }

    /** PUT /api/kafka/topicos/{nombre}/config */
    @PutMapping("/topicos/{nombre}/config")
    public ResponseEntity<Map<String, String>> modificar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String nombre,
            @Valid @RequestBody TopicoConfigRequest request) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(403).build();
        }

        kafkaAdminService.modificarConfiguracion(nombre, request);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Configuración actualizada",
                "nombre", nombre));
    }

    /** DELETE /api/kafka/topicos/{nombre} */
    @DeleteMapping("/topicos/{nombre}")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String nombre) {

        if (!esAdmin(jwt)) {
            return ResponseEntity.status(403).build();
        }

        kafkaAdminService.eliminarTopico(nombre);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/kafka/grupos */
    @GetMapping("/grupos")
    public ResponseEntity<List<ConsumerGroupInfo>> grupos(@AuthenticationPrincipal Jwt jwt) {
        if (!esAdmin(jwt)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(kafkaAdminService.listarGruposConLag());
    }

    private boolean esAdmin(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("Admin");
    }
}
