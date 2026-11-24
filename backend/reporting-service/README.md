# reporting-service

Microservicio de reportes y auditoría de Pedidos360 (puerto **8083**).

Consume los dos tópicos de Kafka y expone los resultados por HTTP. Los datos
se guardan **en memoria**: si el servicio se reinicia, el estado se pierde y el
grupo continúa desde su último offset confirmado, por lo que no reconstruye
automáticamente todo el histórico. Para hacerlo hay que resetear los offsets o
usar un `group.id` nuevo.

## Consumidores

| Grupo | Tópico | Función |
|---|---|---|
| `reporteria-group` | `pedidos.eventos` | KPIs de pedidos y unidades |
| `auditoria-group` | `pedidos.eventos` | Línea de tiempo de cambios |
| `auditoria-group` | `stock.eventos` | Último stock por producto |

Cada grupo recibe su propia copia de los mensajes. Por eso los KPIs y la
auditoría no compiten entre sí.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/reportes/kpis` | Pedidos creados, unidades, cancelaciones, entregas y promedio |
| GET | `/api/reportes/auditoria?limite=50` | Últimos eventos (máximo 100, más reciente primero) |
| GET | `/api/reportes/stock` | Último stock de todos los productos |
| GET | `/api/reportes/stock/{productoId}` | Último stock de un producto (404 si no llegó) |

Todos exigen JWT con scope `Pedidos.Read` y rol **Admin** u **Operador**.

## Ejemplo de KPIs

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

## Variables

Copiar `application-example.properties` a `application.properties` y completar:

- `AZURE_TENANT_ISSUER_URI`
- `AZURE_API_CLIENT_ID`
- `KAFKA_BOOTSTRAP_SERVERS` (por defecto `localhost:9092,localhost:9093,localhost:9094`)

## Ejecutar

```bash
cd backend/reporting-service
mvn spring-boot:run
```

## Notas

- No usa base de datos: todo el estado vive en memoria y está protegido con
  métodos `synchronized`.
- La línea de tiempo conserva los últimos 100 eventos.
- Las dos fábricas de consumidores existen porque `pedidos.eventos` y
  `stock.eventos` tienen DTOs distintos y no se envían cabeceras de tipo.
