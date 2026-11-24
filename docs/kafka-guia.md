# Kafka en Pedidos360 — guía técnica y de defensa

Documento de apoyo para la entrega EP3. Explica qué se implementó, cómo se
opera, cómo se prueba y qué decisiones se tomaron. RabbitMQ sigue existiendo
para notificaciones; Kafka se suma para reportería y auditoría.

---

## 1. Qué se implementó

| Componente | Puerto | Función |
|---|---:|---|
| `orders-service` | 8081 | Publica cada cambio de pedido en `pedidos.eventos` |
| `catalog-service` | 8082 | Publica el stock actualizado en `stock.eventos` |
| `reporting-service` | 8083 | Consume ambos tópicos y expone KPIs, auditoría y stock |
| `kafka-admin-service` | 8084 | Administra tópicos y consulta grupos con `AdminClient` |
| Kafka UI | 8085 | Interfaz web para ver tópicos, mensajes y grupos |
| Clúster Kafka | 9092-9094 | 3 brokers + 3 ZooKeeper en `infra/docker-compose.yml` |

No se reemplazó RabbitMQ. `orders-service` publica el mismo cambio por los dos
canales:

- **RabbitMQ** → `notifications-service` (notificación al cliente).
- **Kafka** → `reporting-service` (KPIs, auditoría y stock).

---

## 2. Arquitectura

```text
                         ┌──────────────────────────────┐
Angular + MSAL ────────► │ AWS API Gateway (JWT/CORS)   │
                         └──────────────┬───────────────┘
                                        │
              ┌─────────────────────────┼─────────────────────────┐
              ▼                         ▼                         ▼
      orders-service            catalog-service           kafka-admin-service
      (8081)                    (8082)                    (8084)
          │                         │                         │
          │ AFTER_COMMIT            │ después del save        │ AdminClient
          ├──────────────┐          │                         │
          ▼              ▼          ▼                         ▼
     RabbitMQ      pedidos.eventos  stock.eventos        Clúster Kafka
     (notifica)     (3 particiones)  (3 particiones)      (3 brokers)
                         │                │
                         │                │
          ┌──────────────┴───────┐        │
          ▼                      ▼        ▼
  reporteria-group        auditoria-group (auditoria-group)
  KPIs en memoria         línea de tiempo + stock
          │                      │
          └──────────┬───────────┘
                     ▼
             reporting-service (8083)
             /api/reportes/kpis
             /api/reportes/auditoria
             /api/reportes/stock
```

---

## 3. Infraestructura

Archivo: `infra/docker-compose.yml`.

- **3 ZooKeeper** (`confluentinc/cp-zookeeper:7.6.1`) con quórum.
- **3 brokers** (`confluentinc/cp-kafka:7.6.1`) en modo ZooKeeper (no KRaft).
- **Kafka UI** (`provectuslabs/kafka-ui`) en el puerto 8085.
- Heaps limitados para que el clúster quepa en una EC2 modesta.

Cada broker tiene dos listeners:

| Listener | Puerto | Uso |
|---|---:|---|
| `INTERNAL` | 29092 | Comunicación entre brokers y Kafka UI dentro de Docker |
| `EXTERNAL` | 9092 / 9093 / 9094 | Microservicios y clientes fuera de Docker |

Configuraciones clave:

- `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`: solo existen los tópicos creados
  por script.
- `KAFKA_MIN_INSYNC_REPLICAS=2`: una escritura necesita al menos dos réplicas
  sincronizadas.
- `KAFKA_DEFAULT_REPLICATION_FACTOR=3`.
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=3`.

Scripts de creación (idempotentes):

```bash
bash infra/scripts/crear-topicos.sh
pwsh infra/scripts/crear-topicos.ps1
```

---

## 4. Tópicos

| Tópico | Particiones | Réplicas | `cleanup.policy` | Retención | Clave |
|---|---:|---:|---|---|---|
| `pedidos.eventos` | 3 | 3 | `delete` | 7 días | `pedidoId` |
| `stock.eventos` | 3 | 3 | `compact` | Infinita (`-1`) | `productoId` |

Ambos con `min.insync.replicas=2`.

### ¿Por qué solo dos tópicos?

El diseño de la entrega pide exactamente dos. No se crean tópicos de Dead
Letter ni tópicos auxiliares. Los errores de consumo se registran en el log y
la recuperación se apoya en los offsets confirmados por cada grupo.

### ¿Por qué `stock.eventos` es compactado?

Porque interesa el **último estado** de cada producto, no todo su historial.
Kafka conserva el último mensaje por clave (`productoId`) y descarta los
anteriores durante la compactación.

### ¿Por qué `pedidos.eventos` usa retención de 7 días?

Porque es un historial de eventos de negocio. La retención acota el disco y
permite reprocesar esos eventos si se reinician los offsets del grupo.

---

## 5. Productores

### `orders-service`

Archivos:

- `messaging/PedidoEventoKafka.java`
- `messaging/PedidoKafkaProducer.java`
- `service/OrderService.java`

`OrderService.crear` y `OrderService.cambiarEstado` publican dos eventos de
Spring: el de RabbitMQ y el de Kafka. `PedidoKafkaProducer` escucha con
`@TransactionalEventListener(phase = AFTER_COMMIT)`, así que el mensaje sale
solo si la transacción de base de datos terminó bien.

Payload:

```json
{
  "pedidoId": 1,
  "estado": "CREADO",
  "cliente": "cliente@ejemplo.com",
  "productoId": 20,
  "cantidad": 2,
  "timestamp": "2026-11-21T16:30:00"
}
```

Clave: `pedidoId`, para que todos los eventos del mismo pedido caigan en la
misma partición y se lean en orden.

### `catalog-service`

Archivos:

- `messaging/StockEventoKafka.java`
- `messaging/StockKafkaProducer.java`
- `service/ProductService.java`

Se publica después de cada `save`: alta, actualización y descuento de stock.

Payload:

```json
{
  "productoId": 20,
  "nombre": "Teclado",
  "stock": 5,
  "precio": 1500,
  "timestamp": "2026-11-21T16:33:00"
}
```

Clave: `productoId`.

Configuración común de los productores:

```properties
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.properties.spring.json.add.type.headers=false
```

---

## 6. Consumidores y grupos

| Grupo | Tópico(s) | Estado que mantiene |
|---|---|---|
| `reporteria-group` | `pedidos.eventos` | KPIs: pedidos creados, unidades, cancelaciones, entregas y promedios |
| `auditoria-group` | `pedidos.eventos`, `stock.eventos` | Línea de tiempo de pedidos y último stock por producto |

Cada grupo recibe su propia copia de los mensajes: los grupos de Kafka son
independientes. Por eso los KPIs y la auditoría no compiten por los mismos
offsets.

### Deserialización

Los productores no envían cabeceras de tipo
(`spring.json.add.type.headers=false`). Por eso `KafkaConsumerConfig` define
dos `ConcurrentKafkaListenerContainerFactory`, cada una con un
`JacksonJsonDeserializer` atado al DTO:

- `pedidoListenerContainerFactory` → `PedidoEventoKafka`
- `stockListenerContainerFactory` → `StockEventoKafka`

Los `@KafkaListener` eligen la factory con `containerFactory`.

### Estado en memoria

- `KpiService`: contadores y mapa de estados.
- `AuditoriaService`: línea de tiempo (máximo 100 eventos) y mapa de stock por
  `productoId`.
- Los métodos son `synchronized` para evitar condiciones de carrera si se
  aumenta la concurrencia.

Al reiniciar el servicio, el estado en memoria se pierde. El grupo conserva
sus offsets, así que el consumidor continúa desde el último evento confirmado
y **no reconstruye automáticamente todo el histórico**. `auto-offset-reset=earliest`
solo aplica cuando el grupo no tiene offsets. Para reconstruir los reportes
habría que resetear los offsets del grupo o usar un `group.id` nuevo.

---

## 7. Flujo completo

1. El cliente crea un pedido en `orders-service`.
2. Se guarda el pedido en la base de datos.
3. Al hacer commit, se publica en RabbitMQ y en `pedidos.eventos`.
4. `reporteria-group` actualiza los KPIs.
5. `auditoria-group` agrega el evento a la línea de tiempo.
6. Al aceptar el pedido, `orders-service` llama a `catalog-service` para
   descontar stock.
7. `catalog-service` guarda el nuevo stock y publica en `stock.eventos`.
8. `auditoria-group` actualiza el último stock del producto.
9. El frontend consulta los reportes por HTTP.

---

## 8. Endpoints

### Reportes (`reporting-service`, 8083)

Requieren JWT con scope `Pedidos.Read` y rol **Admin** u **Operador**.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/reportes/kpis` | Pedidos creados, unidades, cancelaciones, entregas, promedios |
| GET | `/api/reportes/auditoria?limite=50` | Últimos eventos (máximo 100) |
| GET | `/api/reportes/stock` | Último stock de todos los productos |
| GET | `/api/reportes/stock/{productoId}` | Último stock de un producto (404 si no existe) |

Ejemplo de KPIs:

```json
{
  "pedidosCreados": 2,
  "unidadesSolicitadas": 5,
  "pedidosCancelados": 1,
  "pedidosEntregados": 1,
  "pedidosPorEstado": {
    "ACEPTADO": 1,
    "CANCELADO": 1,
    "CREADO": 2,
    "ENTREGADO": 1
  },
  "tasaCancelacionPorcentaje": 50.0,
  "promedioUnidadesPorPedido": 2.5,
  "ultimaActualizacion": "2026-11-21T16:30:00"
}
```

### Administración (`kafka-admin-service`, 8084)

Requieren JWT con scope `Pedidos.Read` y rol **Admin**.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/kafka/topicos` | Crea un tópico |
| GET | `/api/kafka/topicos` | Lista tópicos |
| GET | `/api/kafka/topicos/{nombre}` | Describe particiones, réplicas, ISR y configuración |
| PUT | `/api/kafka/topicos/{nombre}/config` | Modifica `retention.ms` y/o `cleanup.policy` |
| DELETE | `/api/kafka/topicos/{nombre}` | Elimina un tópico |
| GET | `/api/kafka/grupos` | Lista grupos con offset actual, offset final y lag |

Ejemplo para crear un tópico:

```json
POST /api/kafka/topicos
{
  "nombre": "prueba.eventos",
  "particiones": 3,
  "replicas": 3,
  "retentionMs": 604800000,
  "cleanupPolicy": "delete",
  "minInSyncReplicas": 2
}
```

---

## 9. Seguridad

- Todos los endpoints nuevos validan el JWT de Entra ID con el mismo
  `issuer-uri`, `audience` y scope `Pedidos.Read` del resto del proyecto.
- `kafka-admin-service` exige rol **Admin** en el controller.
- `reporting-service` exige rol **Admin** u **Operador**.
- No hay credenciales de Kafka en Git: la infraestructura usa `PLAINTEXT` solo
  en la red del laboratorio y las variables se manejan por entorno.
- `infra/.env` y `application.properties` están ignorados por Git.

---

## 10. Configuración

### Kafka local

```bash
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml --env-file infra/.env up -d
bash infra/scripts/crear-topicos.sh
```

Variables:

| Variable | Uso | Valor local |
|---|---|---|
| `EXTERNAL_HOST` | Host que los brokers anuncian a clientes externos | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers que usan los microservicios | `localhost:9092,localhost:9093,localhost:9094` |
| `KAFKA_UI_PORT` | Puerto de Kafka UI | `8085` |

### Microservicios

`orders-service`, `catalog-service`, `reporting-service` y
`kafka-admin-service` leen `KAFKA_BOOTSTRAP_SERVERS`. Los ejemplos están en sus
`application-example.properties`.

### EC2

Con los microservicios y Kafka en la misma instancia:

```properties
EXTERNAL_HOST=localhost
KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094
```

Si Postman u otra máquina se conecta desde fuera:

1. Cambiar `EXTERNAL_HOST` por la IP pública actual.
2. Volver a ejecutar `docker compose ... up -d`.
3. Abrir en el Security Group los puertos **9092, 9093, 9094** (Kafka),
   **8085** (Kafka UI), **8083** (reportes) y **8084** (administración),
   además de los puertos ya usados por pedidos y catálogo.

La IP pública del Learner Lab cambia al reiniciar: hay que actualizar
`infra/.env` y recrear los contenedores.

La guía completa de despliegue (Security Group, systemd, secretos y API
Gateway) está en [`docs/despliegue-ec2.md`](despliegue-ec2.md).

---

## 11. Pruebas

### Unitarias

| Módulo | Cantidad | Qué cubren |
|---|---:|---|
| `orders-service` | 9 | Pedidos, eventos Rabbit/Kafka, productor Kafka |
| `catalog-service` | 5 | Catálogo, productor de stock |
| `kafka-admin-service` | 10 | DTOs, servicio con `AdminClient`, controller y 403 |
| `reporting-service` | 11 | KPIs, auditoría, controller y flujo de integración |

Total: **35 tests**.

### Integración

`reporting-service/src/test/java/.../integration/FlujoKafkaIntegrationTests.java`

- Levanta un broker Kafka embebido (`@EmbeddedKafka`).
- Crea `pedidos.eventos` y `stock.eventos` con 3 particiones.
- Publica con el mismo `JacksonJsonSerializer` de los microservicios.
- Ejecuta los `@KafkaListener` reales.
- Verifica KPIs, auditoría y stock.
- Usa `AdminClient` para comprobar que `reporteria-group` y `auditoria-group`
  existen y terminan con **lag 0**.

Ejecutar:

```bash
cd backend/reporting-service
./mvnw test
```

Resultado esperado: `Tests run: 11, Failures: 0, Errors: 0`.

### Verificación manual con el clúster real

```bash
docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-consumer-groups --bootstrap-server broker1:29092 \
  --describe --group reporteria-group

docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-consumer-groups --bootstrap-server broker1:29092 \
  --describe --group auditoria-group
```

Se espera `LAG 0` en las particiones consumidas. También se puede consultar
`GET http://localhost:8084/api/kafka/grupos` con un token Admin.

---

## 12. Decisiones de diseño

1. **Dos canales, no reemplazo:** RabbitMQ sigue siendo el canal de
   notificaciones; Kafka se usa para analítica y auditoría.
2. **Evento publicado después del commit:** evita reportar pedidos que la
   transacción terminó revirtiendo.
3. **Clave por entidad:** `pedidoId` y `productoId` garantizan orden por
   entidad y habilitan la compactación de stock.
4. **Dos grupos:** permiten que KPIs y auditoría evolucionen por separado y
   reprocesen sin afectarse.
5. **Estado en memoria:** suficiente para el volumen académico y fácil de
   defender. En producción se reemplazaría por una base de datos o Kafka
   Streams.
6. **DTOs duplicados por servicio:** no se creó un módulo `common`; cada
   servicio es autónomo y el contrato es el JSON.
7. **Sin Dead Letter Topic:** el diseño pide dos tópicos. Los errores se
   registran en el log y los offsets permiten reprocesar.
8. **ZooKeeper en vez de KRaft:** el laboratorio y la imagen elegida usan
   ZooKeeper; KRaft sería la alternativa moderna.
9. **Tres particiones y factor de réplica 3:** permite tolerar la caída de un
   broker y repartir carga entre consumidores.
10. **Kafka UI:** facilita la demostración visual sin instalar herramientas
    adicionales.

---

## 13. Limitaciones y trabajo futuro

- **Entrega al menos una vez:** si un consumidor se reinicia después de
  procesar y antes de confirmar el offset, puede repetir un evento. Los KPIs
  podrían contar un pedido dos veces. Una mejora sería deduplicar por
  `pedidoId + estado` o incluir un `eventoId`.
- **Estado en memoria:** se pierde al reiniciar. El grupo conserva sus offsets,
  por lo que no se reconstruye el histórico automáticamente; para hacerlo hay
  que resetear los offsets o usar un grupo nuevo. No hay consultas históricas
  más allá de la retención de 7 días.
- **Sin DLT:** un mensaje con JSON inválido puede bloquear la partición según
  la configuración de reintentos. Se podría agregar un `DefaultErrorHandler`
  con reintentos y un tópico de errores, pero el diseño pide dos tópicos.
- **Sin autenticación SASL/SSL en Kafka:** la infraestructura es de
  laboratorio. En producción se usaría SASL/SCRAM o mTLS y red privada.
- **RabbitMQ no cumple el escenario completo del enunciado:** el compose
  actual tiene 1 broker y 1 cola principal, no 2 nodos, 3 colas y exchange
  `topic`. Se documenta como brecha separada de Kafka.

---

## 14. Guion corto para la demo

1. Mostrar `docker compose ps`: 3 ZooKeeper, 3 brokers y Kafka UI.
2. Mostrar Kafka UI con los dos tópicos y sus 3 particiones.
3. Abrir un consumidor de `pedidos.eventos` en una terminal.
4. Crear un pedido desde el frontend o Postman.
5. Mostrar el JSON publicado en Kafka.
6. Consultar `GET /api/reportes/kpis` y ver el pedido contado.
7. Consultar `GET /api/reportes/auditoria` y ver la entrada.
8. Aceptar el pedido, mostrar el descuento de stock y el evento en
   `stock.eventos`.
9. Consultar `GET /api/reportes/stock/20`.
10. Mostrar `GET /api/kafka/grupos` con `reporteria-group` y
    `auditoria-group` y su lag.
11. Mostrar el test de integración en verde.

---

## 15. Preguntas probables de la defensa

**¿Por qué Kafka si ya hay RabbitMQ?**
Porque resuelven problemas distintos. RabbitMQ enruta tareas hacia un
consumidor (notificar); Kafka retiene un historial de eventos que varios
grupos pueden leer a su ritmo (reportes y auditoría).

**¿Qué pasa si Kafka está caído?**
El pedido se guarda igual. El productor registra el error en el log y el
`send` es asíncrono, así que la respuesta HTTP no se cae. La notificación por
RabbitMQ sigue su curso independiente.

**¿Cómo se evita publicar un pedido que falló?**
Con `@TransactionalEventListener(AFTER_COMMIT)`: el evento se envía solo
después de que la transacción confirma.

**¿Cómo se garantiza el orden?**
La clave es el `pedidoId`. Kafka envía todos los mensajes con la misma clave a
la misma partición, y dentro de una partición el orden se conserva.

**¿Por qué dos consumer groups?**
Porque cada grupo tiene sus propios offsets. `reporteria-group` puede
reprocesar sin mover los offsets de `auditoria-group`, y viceversa.

**¿Qué pasa al reiniciar `reporting-service`?**
El estado en memoria se pierde y el grupo continúa desde su último offset
confirmado, no desde el inicio. Para reconstruir el histórico hay que resetear
los offsets del grupo o usar un `group.id` nuevo; `auto-offset-reset=earliest`
solo se usa cuando no existe un offset previo.

**¿Por qué `stock.eventos` es compactado?**
Porque solo interesa el último stock por producto. La compactación conserva
la última versión de cada clave y libera espacio de las anteriores.

**¿Qué falta para producción?**
Autenticación SASL/SSL, más réplicas y observabilidad, deduplicación de
eventos, persistencia de reportes y un manejo de errores con reintentos/DLT.
