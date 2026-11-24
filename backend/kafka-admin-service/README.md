# kafka-admin-service

Microservicio de administración de Kafka de Pedidos360 (puerto **8084**).

Usa `AdminClient` de kafka-clients para crear, listar, describir, modificar y
eliminar tópicos, y para consultar consumer groups con sus offsets y lag.
Todos los endpoints exigen JWT válido y rol **Admin**.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/kafka/topicos` | Crea un tópico |
| GET | `/api/kafka/topicos` | Lista tópicos |
| GET | `/api/kafka/topicos/{nombre}` | Describe un tópico (particiones, réplicas, ISR, configs) |
| PUT | `/api/kafka/topicos/{nombre}/config` | Modifica `retention.ms` y/o `cleanup.policy` |
| DELETE | `/api/kafka/topicos/{nombre}` | Elimina un tópico |
| GET | `/api/kafka/grupos` | Lista consumer groups con offsets y lag |

## Crear un tópico

```json
POST /api/kafka/topicos
{
  "nombre": "pedidos.eventos",
  "particiones": 3,
  "replicas": 3,
  "retentionMs": 604800000,
  "cleanupPolicy": "delete",
  "minInSyncReplicas": 2
}
```

## Modificar configuración

```json
PUT /api/kafka/topicos/stock.eventos/config
{
  "retentionMs": -1,
  "cleanupPolicy": "compact"
}
```

## Variables

Copiar `application-example.properties` a `application.properties` y completar:

- `AZURE_TENANT_ISSUER_URI`
- `AZURE_API_CLIENT_ID`
- `KAFKA_BOOTSTRAP_SERVERS` (por defecto `localhost:9092,localhost:9093,localhost:9094`)

## Ejecutar

```bash
cd backend/kafka-admin-service
mvn spring-boot:run
# o con el wrapper local: ./mvnw spring-boot:run
```

## Notas

- `GET /api/kafka/grupos` calcula el lag como `log end offset - offset actual`.
- Si un tópico no existe, responde **404**; si los datos del DTO son inválidos, **400**.
- La seguridad reutiliza el mismo scope `Pedidos.Read` y el claim `roles` del resto del proyecto.
