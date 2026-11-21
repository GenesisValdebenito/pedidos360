# Contraste EP1/EP2 → EP3: administración, confiabilidad RabbitMQ y Kafka

Documento de defensa para la pregunta D4: qué falta para completar las
funciones de administración y confiabilidad de RabbitMQ, y el contraste con
EP3 (Reportería y Auditoría con Kafka).

---

## 1. Estado actual

### RabbitMQ — administración (ya implementado)
- `controller/QueueController`: `POST /api/queues` y `DELETE /api/queues/{name}`.
- `admin/RabbitAdminService`: `crearCola`, `eliminarCola`, `existeCola`.
- `dto/QueueRequest`: validación `@NotBlank`.
- Tests: `QueueControllerTests`, `RabbitAdminServiceTests`, `QueueRequestTests`.

### RabbitMQ — confiabilidad (ya implementado)
- ACK manual: `NotificacionConsumer` con `basicAck` y `basicNack(requeue=false)`.
- DLX `pedidos360.direct.dlx` y DLQ `notificaciones.dlq`.
- `spring.rabbitmq.listener.simple.acknowledge-mode=manual`.
- Tests: `NotificacionConsumerTests`, `RabbitMQConfigTests`.

---

## 2. RabbitMQ — pasos que faltan

### 2.1 Administración (complemento de D3)
| Falta | Clase / cambio | Prueba |
|---|---|---|
| Administrar exchanges y bindings desde HTTP | `ExchangeController`, `BindingController` (`POST /api/exchanges`, `POST /api/bindings`) | `ExchangeControllerTests`, `BindingControllerTests` |
| Listar colas y su profundidad | `RabbitAdminService.listarColas()` usando `getQueueProperties` | `RabbitAdminServiceTests` |
| Purgar una cola | `RabbitAdminService.purgarCola(name)` | `RabbitAdminServiceTests` |
| Seguridad del endpoint | Restringir a rol Admin o token interno (hoy está abierto y debe quedar en red interna) | `QueueControllerTests` con contexto de seguridad |
| DTOs de exchange/binding | `ExchangeRequest`, `BindingRequest` | Tests de validación |

### 2.2 Confiabilidad (complemento de D2)
| Falta | Clase / cambio | Prueba |
|---|---|---|
| Reintentos con backoff antes de la DLQ | `spring.rabbitmq.listener.simple.retry.*` o `RepublishMessageRecoverer` | `NotificacionConsumerTests` (simula N intentos) |
| No reencolar por defecto | `default-requeue-rejected=false` | Test de config |
| Prefetch acotado | `spring.rabbitmq.listener.simple.prefetch=10` | Test de config |
| Idempotencia (no procesar dos veces el mismo evento) | `ProcesadorNotificacion` con deduplicación por `pedidoId + estado` | `ProcesadorNotificacionTests` |
| Consumidor de DLQ (carta de letras) | `DlqConsumer` que registra/alerta | `DlqConsumerTests` |
| Prueba de integración real | Testcontainers con RabbitMQ | `NotificacionIntegrationTests` |

---

## 3. EP3 — Reportería y Auditoría con Kafka

Hoy no existe Kafka en el proyecto (solo menciones en `README.md` y `docs/`).
Para EP3 faltan, de punta a punta:

### 3.1 Productor de auditoría
- `AuditEvento` (registro): `pedidoId`, `estadoAnterior`, `estadoNuevo`, `actor`, `timestamp`, `traceId`.
- `AuditEventoProducer`: publica en el topic `pedidos360.audit.events` con `KafkaTemplate`, de forma transaccional (`@TransactionalEventListener(AFTER_COMMIT)`, igual que `PedidoEventoProducer`).
- Se engancha en `OrderService.cambiarEstado` y `crear`.

### 3.2 Consumo y almacenamiento
- `AuditEventoConsumer`: lee el topic y persiste en `audit_events`.
- `AuditEvent` (entidad JPA) + `AuditEventRepository`.
- Opcional: Elasticsearch/OpenSearch para consultas analíticas.

### 3.3 Reportería
- `AuditReportController`: `GET /api/reports/audit?fechaDesde=&fechaHasta=&actor=`.
- `AuditReportService`: agregaciones (cambios por día, por actor, por estado).
- Dashboard o exportación CSV.

### 3.4 Infraestructura
- `docker-compose` con Kafka (o Redpanda para local) y, en AWS, Amazon MSK.
- `resilience4j-retry` / Dead Letter Topic de Kafka para errores de consumo.

### 3.5 Pruebas que se incorporarían
- `AuditEventoProducerTests`: verifica topic, clave y payload.
- `AuditEventoConsumerTests`: verifica persistencia e idempotencia.
- `AuditReportServiceTests`: verifica agregaciones.
- Test de integración con Testcontainers Kafka.

---

## 4. Conclusión para la defensa

- **EP1 (actual):** RabbitMQ para notificaciones, con exchanges, bindings,
  routing keys, ACK manual y DLQ. Administración de colas vía REST.
- **EP3 (futuro):** Kafka para auditoría y reportería, con productor de
  eventos, consumo persistente, consultas agregadas y pruebas de integración.
- Lo que **queda inmediatamente** para cerrar el margen de RabbitMQ:
  reintentos, prefetch, idempotencia, consumidor de DLQ y ampliación de la
  administración a exchanges/bindings.
