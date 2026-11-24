# Contraste EP1/EP2 → EP3: RabbitMQ y Kafka

Documento de defensa para la pregunta D4: qué se implementó para EP3
(Reportería y Auditoría con Kafka), cómo se relaciona con lo ya construido
sobre RabbitMQ y qué brechas quedan abiertas de forma consciente.

---

## 1. Resumen

- **RabbitMQ** sigue siendo el canal de notificaciones de `orders-service`
  hacia `notifications-service`. No se reemplazó ni se debilitó.
- **Kafka** se agregó como segundo canal, orientado a reportería y auditoría.
  `orders-service` y `catalog-service` publican eventos; `reporting-service`
  los consume y `kafka-admin-service` administra el clúster.
- Los dos canales conviven: el mismo cambio de pedido se publica por RabbitMQ
  (notificación al cliente) y por Kafka (analítica y trazabilidad).

```text
orders-service ──► RabbitMQ ──► notifications-service   (notificación)
              └──► Kafka    ──► reporting-service        (KPIs y auditoría)
catalog-service ──► Kafka    ──► reporting-service        (stock)
```

---

## 2. Qué se implementó para EP3

### 2.1 Infraestructura
- `infra/docker-compose.yml`: 3 ZooKeeper, 3 brokers y Kafka UI.
- Listeners separados: `INTERNAL://brokerN:29092` para los contenedores y
  `EXTERNAL://<host>:9092/9093/9094` para los microservicios.
- `AUTO_CREATE_TOPICS=false` y `min.insync.replicas=2`.
- Scripts `infra/scripts/crear-topicos.sh` y `.ps1`.
- Variables de infraestructura en `infra/.env.example` (el `.env` real no se
  versiona).

### 2.2 Tópicos
| Tópico | Particiones | Réplicas | Limpieza | Clave |
|---|---:|---:|---|---|
| `pedidos.eventos` | 3 | 3 | `delete`, 7 días | `pedidoId` |
| `stock.eventos` | 3 | 3 | `compact` | `productoId` |

Son exactamente dos tópicos: el diseño no usa Dead Letter Topic ni tópicos
auxiliares.

### 2.3 Productores
- `orders-service` → `PedidoKafkaProducer`, invocado con
  `@TransactionalEventListener(AFTER_COMMIT)` para no publicar pedidos cuya
  transacción se revirtió.
- `catalog-service` → `StockKafkaProducer`, invocado después de cada `save`
  de stock.
- Serialización JSON con `JacksonJsonSerializer`, `acks=all` e idempotencia
  habilitada.

### 2.4 Consumidores
`reporting-service` usa dos grupos independientes:

| Grupo | Tópicos | Función |
|---|---|---|
| `reporteria-group` | `pedidos.eventos` | KPIs de pedidos y unidades |
| `auditoria-group` | `pedidos.eventos`, `stock.eventos` | Línea de tiempo y último stock por producto |

Cada grupo mantiene su propio offset, por lo que puede reprocesar o escalar
sin afectar al otro. El estado es en memoria: al reiniciar se pierde y el grupo
continúa desde su último offset confirmado.

### 2.5 Administración
`kafka-admin-service` usa `AdminClient` para:

- Crear, listar, describir, modificar y eliminar tópicos.
- Consultar consumer groups con offset actual, offset final y **lag**.

### 2.6 Seguridad
- Los endpoints de reportes exigen `SCOPE_Pedidos.Read` y rol Admin u
  Operador.
- La administración de Kafka exige rol Admin.
- Se reutiliza el mismo emisor y claim `roles` de Entra ID que en EP1/EP2; no
  se agregó una seguridad paralela ni se relajaron los 401/403.

### 2.7 Pruebas
| Servicio | Pruebas |
|---|---:|
| `orders-service` | 9 |
| `catalog-service` | 5 |
| `kafka-admin-service` | 10 |
| `reporting-service` | 11 |
| **Total** | **35** |

Incluye `FlujoKafkaIntegrationTests`, que levanta un broker Kafka embebido,
publica eventos, verifica el consumo por ambos grupos y comprueba lag 0.

---

## 3. Mapa requisito EP3 → evidencia

| Requisito | Dónde está | Cómo se demuestra |
|---|---|---|
| Productor de eventos de pedido | `PedidoKafkaProducer` | Crear/aceptar un pedido y ver el JSON en `pedidos.eventos` |
| Productor de eventos de stock | `StockKafkaProducer` | Aceptar un pedido y ver el evento en `stock.eventos` |
| Consumo con grupos | `reporting-service` | `GET /api/kafka/grupos` muestra ambos grupos |
| Reportería por HTTP | `ReporteController` | `GET /api/reportes/kpis` y `/auditoria` |
| Stock consultable | `StockController` | `GET /api/reportes/stock/{productoId}` |
| Administración de tópicos | `KafkaAdminService` | CRUD en `/api/kafka/topicos` |
| Observabilidad del lag | `KafkaAdminService.listarGrupos` | `GET /api/kafka/grupos` |
| Confiabilidad de publicación | `AFTER_COMMIT`, `acks=all`, idempotencia | Reiniciar antes del commit o simular error |
| Infraestructura replicada | `infra/docker-compose.yml` | 3 brokers `healthy` y tópicos con RF 3 |
| Prueba automatizada | `FlujoKafkaIntegrationTests` | `mvn test` en verde |

---

## 4. RabbitMQ: estado y brechas

### 4.1 Lo que ya está resuelto
- `pedidos360.direct.exchange` con routing keys por estado (`pedido.creado`,
  `pedido.aceptado`, etc.).
- `notificaciones.queue` con **ACK manual** (`basicAck` / `basicNack`).
- DLX `pedidos360.direct.dlx` y DLQ `notificaciones.dlq`.
- Administración de colas por REST (`POST /api/queues`,
  `DELETE /api/queues/{name}`).
- Pruebas de configuración, consumidor y administración.

### 4.2 Brechas frente al escenario completo
| Brecha | Estado actual | Qué faltaría |
|---|---|---|
| Alta disponibilidad | 1 broker en `docker-compose.yml` | 2 nodos RabbitMQ en clúster |
| Cantidad de colas | 1 cola principal + 1 DLQ | 3 colas funcionales |
| Tipo de exchange | `direct` | `topic` con patrones de routing |
| Administración de exchanges/bindings | Solo colas | `ExchangeController` y `BindingController` |
| Reintentos | NACK directo a DLQ | Backoff y `RepublishMessageRecoverer` |
| Prefetch | Valor por defecto | `prefetch=10` explícito |
| Idempotencia | No implementada | Deduplicación por `pedidoId + estado` |
| Consumidor de DLQ | No existe | Alertas o reencolado controlado |
| Prueba de integración | Mocks/unitarias | Testcontainers con RabbitMQ |

Estas brechas se dejan documentadas y **no se mezclan con Kafka**: el
enunciado de EP3 pide Kafka para reportería y auditoría, y RabbitMQ conserva
su rol de notificaciones.

---

## 5. Decisiones y justificación

1. **Dos canales, no reemplazo.** Eliminar RabbitMQ habría roto EP2 y la
   notificación al cliente. Kafka resuelve otra necesidad: retención,
   reprocesamiento y varios consumidores independientes.
2. **Evento después del commit.** Evita que reportería cuente pedidos que la
   base de datos terminó revirtiendo.
3. **Clave por entidad.** `pedidoId` y `productoId` garantizan orden dentro de
   la partición y habilitan la compactación de stock.
4. **Dos grupos.** KPIs y auditoría tienen ciclos de vida distintos; separarlos
   permite reprocesar uno sin afectar al otro.
5. **Dos tópicos y sin DLT.** Es el alcance solicitado. Los errores quedan en
   el log y el offset permite reprocesar; una DLT se documenta como trabajo
   futuro, no como requisito.
6. **Estado en memoria.** Suficiente para el volumen académico y fácil de
   defender. En producción se usaría una base de datos o Kafka Streams.
7. **DTOs duplicados por servicio.** No se creó un módulo `common`: cada
   servicio es autónomo y el contrato es el JSON del evento.
8. **ZooKeeper en vez de KRaft.** Es el modo soportado por la imagen elegida
   (`confluentinc/cp-kafka:7.6.1`) y simplifica la demo.
9. **Tres particiones y RF 3.** Tolera la caída de un broker y permite
   repartir carga entre consumidores del mismo grupo.

---

## 6. Limitaciones de Kafka

- **Entrega al menos una vez:** un reinicio entre el procesamiento y el commit
  del offset puede repetir un evento. Los KPIs podrían contar dos veces el
  mismo pedido; la mejora es deduplicar por `pedidoId + estado` o incluir un
  `eventoId`.
- **Estado en memoria:** se pierde al reiniciar y el grupo continúa desde su
  último offset; no se reconstruye el histórico automáticamente. Se resolvería
  reseteando offsets, usando un grupo nuevo o persistiendo el estado.
- **Sin DLT:** un JSON inválido puede bloquear la partición según los
  reintentos configurados. Se resolvería con `DefaultErrorHandler` y un tópico
  de errores, fuera del alcance de dos tópicos.
- **Sin SASL/SSL:** el clúster es de laboratorio (`PLAINTEXT`). En producción
  se usaría SASL/SCRAM o mTLS y una red privada.
- **Kafka UI sin autenticación:** está pensada para la demo local; no debe
  exponerse a Internet.

---

## 7. Conclusión para la defensa

- **EP1/EP2:** RabbitMQ para notificaciones, con exchange, bindings, ACK
  manual, DLQ y administración de colas por REST.
- **EP3:** Kafka para reportería y auditoría, con dos tópicos replicados,
  productores después del commit, dos grupos de consumo, reportes HTTP,
  administración con `AdminClient`, seguridad por rol y 35 pruebas.
- **Conviviencia:** ambos canales se complementan; no hay reemplazo ni
  duplicación de responsabilidades.
- **Brechas declaradas:** alta disponibilidad y tercer cola en RabbitMQ,
  reintentos/idempotencia y DLT en Kafka. Se documentan con su mejora concreta
  en lugar de ocultarlas.
