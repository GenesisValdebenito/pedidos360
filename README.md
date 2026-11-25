# Pedidos360 — DSY1107

Sistema de gestión de pedidos y catálogo con arquitectura cloud native.
Proyecto académico — **DSY1107 · Desarrollo Cloud Native I**
Evaluaciones: **EP1 — Encargo (16%)**, **EP2 — Presentación (24%)** y **EP3 — Reportería y Auditoría con Kafka**

**Autora:** @GenesisValdebenito
**Trabajo:** Individual

---

## 1. Descripción

Pedidos360 es un sistema que permite gestionar pedidos y catálogo de productos, integrando:

- **Frontend:** Angular + MSAL (login con Microsoft Entra ID)
- **Backend:** Microservicios en Spring Boot (Pedidos, Catálogo, Notificaciones, Reportería y Administración de Kafka), desplegados en AWS EC2
- **Mensajería:** RabbitMQ para notificaciones de pedidos y Kafka para reportería y auditoría
- **Gateway:** AWS API Gateway con validación JWT y CORS
- **Persistencia:** Base de datos cloud

## 2. Arquitectura

```
USUARIO
  ↓
ANGULAR + MSAL
  ↓
MICROSOFT ENTRA ID  →  Access Token (JWT)
  ↓
AWS API GATEWAY  (JWT Authorizer + CORS)
  ↓
SPRING BOOT + SPRING SECURITY
  ↓
PEDIDOS + CATÁLOGO
  ├─→ BASE DE DATOS CLOUD
  ├─→ RabbitMQ → notifications-service
  └─→ Kafka
        ├─ pedidos.eventos → reporteria-group   (KPIs)
        └─ pedidos.eventos / stock.eventos
              → auditoria-group                (auditoría + stock)
                    ↓
             reporting-service (reportes HTTP)
```

## 3. Estructura del repositorio

```
pedidos360-dsy1107/
├── frontend/                    # Angular + MSAL
├── backend/
│   ├── orders-service/          # Gestión de Pedidos (publica en RabbitMQ y Kafka)
│   ├── catalog-service/         # Catálogo / Stock (publica en Kafka)
│   ├── notifications-service/   # Consumidor RabbitMQ
│   ├── reporting-service/       # KPIs, auditoría y stock desde Kafka
│   └── kafka-admin-service/     # Administración de tópicos y grupos
├── infra/
│   ├── docker-compose.yml       # 3 ZooKeeper + 3 brokers + Kafka UI
│   ├── .env.example
│   ├── ec2/                     # variables de entorno para EC2
│   ├── systemd/                 # unidad pedidos360@.service
│   └── scripts/                 # creación de tópicos y despliegue
├── docker-compose.yml           # RabbitMQ local
├── docs/                        # Guías, diagramas y documentación de defensa
├── .gitignore
└── README.md
```

## 4. Módulos funcionales

### 4.1 Gestión de Pedidos
- Crear, listar y consultar pedidos.
- Cambiar estado siguiendo esta secuencia: `CREADO → ACEPTADO → EN_PREPARACION → DESPACHADO → ENTREGADO`.
- Un pedido solo se puede cancelar mientras está en `CREADO`; `CANCELADO` y `ENTREGADO` son estados finales.
- El stock se descuenta una sola vez, al aceptar un pedido que estaba en `CREADO`.
- Admin y Operador pueden consultar cualquier pedido; Cliente solo puede consultar pedidos propios.

### 4.2 Catálogo de Productos / Stock
- Listar productos disponibles.
- Crear y editar productos (solo Admin).
- Regla de negocio: al **aceptar** un pedido, el **stock disminuye** según los productos del pedido.
- El endpoint interno de descuento requiere una credencial compartida entre `orders-service` y `catalog-service`, configurada en `PEDIDOS360_INTERNAL_TOKEN`.

### 4.3 Notificaciones asíncronas
- `orders-service` publica un evento después de guardar un pedido creado o un cambio de estado válido.
- RabbitMQ enruta los eventos mediante `pedidos360.direct.exchange` y routing keys como `pedido.creado` y `pedido.aceptado`.
- `notifications-service` consume `notificaciones.queue` con **ACK manual** (`basicAck`/`basicNack`) y registra el evento en sus logs. La validación de stock continúa siendo síncrona.
- Si el procesamiento falla, el consumidor hace `basicNack(requeue=false)` y el mensaje cae en la **DLQ** (`notificaciones.dlq`) a través del DLX `pedidos360.direct.dlx`.
- El endpoint de administración `POST /api/queues` y `DELETE /api/queues/{name}` permite crear y eliminar colas en caliente.

### 4.4 Reportería y auditoría con Kafka
- `orders-service` publica en `pedidos.eventos` después del commit, con clave `pedidoId`.
- `catalog-service` publica en `stock.eventos` después de cada cambio de stock, con clave `productoId`.
- `reporting-service` consume con dos grupos independientes:
  - `reporteria-group`: KPIs de pedidos y unidades.
  - `auditoria-group`: línea de tiempo de pedidos y último stock por producto.
- Los tópicos tienen 3 particiones y factor de réplica 3. `pedidos.eventos` usa retención de 7 días y `stock.eventos` es compactado.
- Los reportes se consultan en `GET /api/reportes/kpis`, `/api/reportes/auditoria` y `/api/reportes/stock`.

### 4.5 Administración de Kafka
- `kafka-admin-service` usa `AdminClient` para crear, listar, describir, modificar y eliminar tópicos.
- `GET /api/kafka/grupos` muestra los consumer groups con su offset actual, offset final y lag.
- Solo el rol Admin puede administrar tópicos.
- La infraestructura local está en `infra/`: 3 ZooKeeper, 3 brokers y Kafka UI.

> Guía completa de Kafka, operación, pruebas y defensa: [docs/kafka-guia.md](docs/kafka-guia.md).

## 5. Roles y permisos

| Acción | Admin | Operador | Cliente |
|---|---|---|---|
| Ver catálogo | Sí | Sí | Sí |
| Crear pedido | Opcional | Sí | Sí |
| Ver pedidos propios | Sí | Sí | Sí |
| Ver todos los pedidos | Sí | Sí | No |
| Cambiar estado de pedido | Opcional | Sí | No |
| Cancelar pedido en CREADO | Sí | Sí | Solo los propios |
| Crear/editar producto | Sí | No | No |
| Administrar stock | Sí | No | No |
| Ver KPIs, auditoría y stock | Sí | Sí | No |
| Administrar tópicos y ver lag en Kafka | Sí | No | No |

## 6. Endpoints principales

```
PEDIDOS
POST   /api/orders
GET    /api/orders
GET    /api/orders/{id}
PUT    /api/orders/{id}/status
PUT    /api/orders/{id}/cancelar

CATÁLOGO
GET    /api/catalog/products
POST   /api/catalog/products
PUT    /api/catalog/products/{id}

REPORTES (Admin/Operador)
GET    /api/reportes/kpis
GET    /api/reportes/auditoria?limite=50
GET    /api/reportes/stock
GET    /api/reportes/stock/{productoId}

ADMINISTRACIÓN DE KAFKA (Admin)
POST   /api/kafka/topicos
GET    /api/kafka/topicos
GET    /api/kafka/topicos/{nombre}
PUT    /api/kafka/topicos/{nombre}/config
DELETE /api/kafka/topicos/{nombre}
GET    /api/kafka/grupos
```

## 7. Cómo ejecutar el proyecto

### Requisitos previos
- Node.js 18+ y npm
- Java 17+ y Maven/./mvnw
- Acceso a Microsoft Entra ID (App Registration para MSAL)
- Base de datos PostgreSQL (Supabase o equivalente)
- Variables de entorno configuradas para la API Gateway y BD

### 1) Configurar variables locales
Copia los archivos de ejemplo sin secretos y completa tus valores reales en tu entorno local:

- Frontend:
  - copiar `frontend/src/environments/environment.example.ts` a `frontend/src/environments/environment.ts`
  - `environment.example.ts` solo contiene placeholders (`YOUR_CLIENT_ID`, `YOUR_TENANT_ID`, `YOUR_API_GATEWAY_URL`, etc.); reemplázalos por tus valores reales en `environment.ts`
  - `environment.ts` está excluido del repositorio (`.gitignore`), por lo que las credenciales nunca se publican
- Backend orders-service:
  - si todavía no existe, copiar `backend/orders-service/src/main/resources/application-example.properties` a `backend/orders-service/src/main/resources/application.properties`
  - si ya existe, conservar sus valores locales y agregarle las propiedades nuevas indicadas en el ejemplo
- Backend catalog-service:
  - si todavía no existe, copiar `backend/catalog-service/src/main/resources/application-example.properties` a `backend/catalog-service/src/main/resources/application.properties`
- Backend notifications-service:
  - copiar `backend/notifications-service/src/main/resources/application-example.properties` a `backend/notifications-service/src/main/resources/application.properties`
- Backend reporting-service:
  - copiar `backend/reporting-service/src/main/resources/application-example.properties` a `backend/reporting-service/src/main/resources/application.properties`
- Backend kafka-admin-service:
  - copiar `backend/kafka-admin-service/src/main/resources/application-example.properties` a `backend/kafka-admin-service/src/main/resources/application.properties`
- Infraestructura Kafka:
  - copiar `infra/.env.example` a `infra/.env`
  - ajustar `EXTERNAL_HOST` según dónde estén los clientes (local o IP pública de EC2)

También puedes usar variables de entorno del sistema, por ejemplo:
- `AZURE_TENANT_ISSUER_URI`
- `AZURE_API_CLIENT_ID`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PEDIDOS360_INTERNAL_TOKEN`
- `PEDIDOS360_CATALOG_URL` (opcional; por defecto `http://localhost:8082`)
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS` (por defecto `localhost:9092,localhost:9093,localhost:9094`)
- `EXTERNAL_HOST` (solo para `infra/.env`; dirección que los brokers anuncian a clientes externos)
- `KAFKA_UI_PORT` (por defecto `8085`)

### 2) Levantar RabbitMQ y Kafka local
Desde la raíz del repositorio, RabbitMQ:
```powershell
docker compose up -d
docker compose ps
```
RabbitMQ debe aparecer como activo/healthy. La consola de administración queda en http://localhost:15672; los puertos AMQP y de administración están enlazados solo a la interfaz local.

Kafka (3 ZooKeeper + 3 brokers + Kafka UI):
```powershell
copy infra\.env.example infra\.env
docker compose -f infra\docker-compose.yml --env-file infra\.env up -d
docker compose -f infra\docker-compose.yml ps
```
Espera a que los tres brokers estén `healthy` y crea los tópicos:
```powershell
pwsh infra\scripts\crear-topicos.ps1
```
En Bash:
```bash
bash infra/scripts/crear-topicos.sh
```
Los tópicos son `pedidos.eventos` (3 particiones, 7 días de retención) y `stock.eventos` (3 particiones, compactado). Kafka UI queda en http://localhost:8085. Detalle completo en [docs/kafka-guia.md](docs/kafka-guia.md).

### 3) Ejecutar frontend (Angular)
```bash
cd frontend
npm install
npm start
```
Si prefieres usar Angular CLI directamente:
```bash
cd frontend
npx ng serve
```
La app quedará disponible en http://localhost:4200

### 4) Ejecutar backend (Spring Boot)
Primero agrega las propiedades de RabbitMQ del archivo `application-example.properties` a tu `application.properties` local de `orders-service`, sin reemplazar las credenciales existentes. Copia `backend/notifications-service/src/main/resources/application-example.properties` a `application.properties` dentro de `notifications-service`.

En PowerShell, inicia RabbitMQ y luego abre terminales separadas:
```powershell
cd backend\notifications-service
mvn spring-boot:run
```
```powershell
cd backend\orders-service
.\mvnw.cmd spring-boot:run
```
```powershell
cd backend\catalog-service
.\mvnw.cmd spring-boot:run
```
```powershell
cd backend\reporting-service
.\mvnw.cmd spring-boot:run
```
```powershell
cd backend\kafka-admin-service
.\mvnw.cmd spring-boot:run
```
Los microservicios quedarán disponibles en:
- Orders: http://localhost:8081
- Catalog: http://localhost:8082
- Reporting: http://localhost:8083
- Kafka Admin: http://localhost:8084
- Notifications: consumidor sin endpoint HTTP
- Kafka UI: http://localhost:8085

Inicia RabbitMQ y `notifications-service` antes de publicar pedidos para que se creen la cola y sus bindings. Inicia Kafka y crea los tópicos antes de arrancar `reporting-service` y `kafka-admin-service`. En AWS EC2, ejecuta RabbitMQ y Kafka en la misma instancia y configura `RABBITMQ_HOST=localhost` y `KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094`; no abras los puertos de RabbitMQ en el Security Group.

### 5) Verificar salud inicial
- Frontend: abrir http://localhost:4200
- Orders service: http://localhost:8081/actuator/health (si está habilitado)
- Catalog service: http://localhost:8082/actuator/health (si está habilitado)
- Reporting service: http://localhost:8083/api/reportes/kpis con un token Admin u Operador
- Kafka Admin: http://localhost:8084/api/kafka/topicos con un token Admin
- Kafka UI: http://localhost:8085

### 6) Despliegue en EC2
La guía completa está en [docs/despliegue-ec2.md](docs/despliegue-ec2.md): instancia recomendada, Security Group, instalación de Docker/Java/Maven, secretos fuera del repositorio, clúster Kafka, systemd y verificación. El resumen es:

```bash
bash infra/scripts/desplegar-ec2.sh --instalar
```

### 7) Recomendaciones de seguridad
- Nunca subir archivos con credenciales reales ni conexiones a BD al repositorio.
- Mantener `application.properties` y `environment.ts` locales y no versionados.
- Usar `.env` o variables de entorno del sistema para valores sensibles.

## 8. Backlog / Progreso del proyecto

### Sesión 1 — Identidad + esqueleto
- [ x] Repositorio y estructura de carpetas creada
- [ x] `.gitignore` configurado
- [ x] App registrada en Microsoft Entra ID (tenant, App Registration, Redirect URI)
- [ x] Usuarios de prueba con roles (Admin/Operador/Cliente)
- [ x] Angular + MSAL: login/logout funcionando
- [ x] Scaffolding de orders-service y catalog-service (compilando)

### Sesión 2 — Backend + seguridad + persistencia
- [ x] BD cloud configurada y conectada
- [ x] Entidades y repositorios: Order, OrderItem, Product
- [ x] Spring Security valida JWT (issuer, audience, firma, expiración)
- [ x] Endpoints de Pedidos (CRUD + cambio de estado + regla ACEPTADO→DESPACHADO)
- [ x] Endpoints de Catálogo (CRUD + descuento de stock al aceptar pedido)
- [ x] API Gateway desplegado con JWT Authorizer + CORS

### Sesión 3 — Frontend + integración + pruebas
- [ x] Pantallas `/dashboard`, `/orders`, `/catalog` conectadas a la API
- [ x] MsalGuard en rutas protegidas + MsalInterceptor adjuntando token
- [ x] Diferenciación por rol funcionando
- [ x] Flujo end-to-end probado (Angular → Gateway → Backend → BD)
- [ x] Casos 200/401/403 probados y documentados
- [ x] Repositorio limpio, sin secretos
- [ x] Recorrido del video EP2 preparado

### Sesión 4 — Kafka, reportería y auditoría
- [ x] Clúster local: 3 ZooKeeper + 3 brokers + Kafka UI (`infra/`)
- [ x] Tópicos `pedidos.eventos` y `stock.eventos` con 3 particiones y RF 3
- [ x] Productor de pedidos después del commit, conviviendo con RabbitMQ
- [ x] Productor de stock en `catalog-service`
- [ x] `reporting-service` con grupos `reporteria-group` y `auditoria-group`
- [ x] KPIs, línea de tiempo y stock por HTTP
- [ x] `kafka-admin-service` con administración de tópicos y lag
- [ x] Prueba de integración con broker Kafka embebido
- [ x] Documentación técnica y de defensa
- [ x] Guía de despliegue EC2 y unidad systemd para los servicios

## 9. Seguridad

- No se suben contraseñas, Client Secret, cadenas de conexión completas ni tokens reutilizables.
- Variables sensibles se manejan por entorno (`.env`, variables de entorno del sistema), nunca hardcodeadas.
- Ver `.gitignore` para archivos excluidos.

## 10. Alcance del proyecto (progresión)

| Módulo | Evaluación | Estado |
|---|---|---|
| Pedidos + Catálogo | **EP1** | Implementado |
| Notificaciones (RabbitMQ) | EP2 | Implementado |
| Reportería y Auditoría (Kafka) | EP3 | Implementado |

> La guía técnica de Kafka está en `docs/kafka-guia.md`.
> El contraste con el escenario EP3, las brechas de RabbitMQ y las decisiones
> tomadas están en `docs/contraste-ep3-kafka.md`.
> El manual de usuario general está en `docs/manual-usuario.md`.

---

*DSY1107 — Desarrollo Cloud Native I — EP1/EP2/EP3*