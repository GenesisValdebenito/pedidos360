# Pedidos360 — DSY1107

Sistema de gestión de pedidos y catálogo con arquitectura cloud native.
Proyecto académico — **DSY1107 · Desarrollo Cloud Native I**
Evaluaciones: **EP1 — Encargo (16%)** + **EP2 — Presentación (24%)**

**Autora:** @GenesisValdebenito
**Trabajo:** Individual

---

## 1. Descripción

Pedidos360 es un sistema que permite gestionar pedidos y catálogo de productos, integrando:

- **Frontend:** Angular + MSAL (login con Microsoft Entra ID)
- **Backend:** Microservicios en Spring Boot (Pedidos, Catálogo y Notificaciones), desplegados en AWS EC2
- **Mensajería:** RabbitMQ para procesar notificaciones de pedidos de forma asíncrona
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
  └─→ RabbitMQ → notifications-service
```

## 3. Estructura del repositorio

```
pedidos360-dsy1107/
├── frontend/              # Angular + MSAL
├── backend/
│   ├── orders-service/    # Gestión de Pedidos
│   ├── catalog-service/   # Catálogo / Stock
│   └── notifications-service/ # Consumidor RabbitMQ
├── docker-compose.yml     # RabbitMQ local
├── docs/                  # Diagramas y capturas de arquitectura
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
- `notifications-service` consume `notificaciones.queue` y registra el evento en sus logs. La validación de stock continúa siendo síncrona.
- En esta iteración no se incluyen DLQ ni ACK manual; se mantienen para la siguiente etapa de confiabilidad de mensajería.

## 5. Roles y permisos

| Acción | Admin | Operador | Cliente |
|---|---|---|---|
| Ver catálogo | Sí | Sí | Sí |
| Crear pedido | Opcional | Sí | Sí |
| Ver pedidos propios | Sí | Sí | Sí |
| Ver todos los pedidos | Sí | Sí | No |
| Cambiar estado de pedido | Opcional | Sí | No |
| Crear/editar producto | Sí | No | No |
| Administrar stock | Sí | No | No |

## 6. Endpoints principales

```
PEDIDOS
POST   /api/orders
GET    /api/orders
GET    /api/orders/{id}
PUT    /api/orders/{id}/status

CATÁLOGO
GET    /api/catalog/products
POST   /api/catalog/products
PUT    /api/catalog/products/{id}
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
- Backend orders-service:
  - si todavía no existe, copiar `backend/orders-service/src/main/resources/application-example.properties` a `backend/orders-service/src/main/resources/application.properties`
  - si ya existe, conservar sus valores locales y agregarle las propiedades nuevas indicadas en el ejemplo
- Backend catalog-service:
  - si todavía no existe, copiar `backend/catalog-service/src/main/resources/application-example.properties` a `backend/catalog-service/src/main/resources/application.properties`
- Backend notifications-service:
  - copiar `backend/notifications-service/src/main/resources/application-example.properties` a `backend/notifications-service/src/main/resources/application.properties`

También puedes usar variables de entorno del sistema, por ejemplo:
- `AZURE_TENANT_ISSUER_URI`
- `AZURE_API_CLIENT_ID`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PEDIDOS360_INTERNAL_TOKEN`
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`

### 2) Levantar RabbitMQ local
Desde la raíz del repositorio:
```powershell
docker compose up -d
docker compose ps
```
RabbitMQ debe aparecer como activo/healthy. La consola de administración queda en http://localhost:15672; los puertos AMQP y de administración están enlazados solo a la interfaz local.

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
Los microservicios quedarán disponibles en:
- Orders: http://localhost:8081
- Catalog: http://localhost:8082
- Notifications: consumidor sin endpoint HTTP

Inicia RabbitMQ y `notifications-service` antes de publicar pedidos para que se creen la cola y sus bindings. En AWS EC2, ejecuta RabbitMQ en la misma instancia y configura `RABBITMQ_HOST=localhost`; no abras los puertos del broker en el Security Group.

### 5) Verificar salud inicial
- Frontend: abrir http://localhost:4200
- Orders service: http://localhost:8081/actuator/health (si está habilitado)
- Catalog service: http://localhost:8082/actuator/health (si está habilitado)

### 6) Recomendaciones de seguridad
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
- [ ]x Spring Security valida JWT (issuer, audience, firma, expiración)
- [ x] Endpoints de Pedidos (CRUD + cambio de estado + regla ACEPTADO→DESPACHADO)
- [ x] Endpoints de Catálogo (CRUD + descuento de stock al aceptar pedido)
- [ x] API Gateway desplegado con JWT Authorizer + CORS

### Sesión 3 — Frontend + integración + pruebas
- [ x] Pantallas `/dashboard`, `/orders`, `/catalog` conectadas a la API
- [ ]x MsalGuard en rutas protegidas + MsalInterceptor adjuntando token
- [ x] Diferenciación por rol funcionando
- [ x] Flujo end-to-end probado (Angular → Gateway → Backend → BD)
- [ x] Casos 200/401/403 probados y documentados
- [ x] Repositorio limpio, sin secretos
- [ x] Recorrido del video EP2 preparado

## 9. Seguridad

- No se suben contraseñas, Client Secret, cadenas de conexión completas ni tokens reutilizables.
- Variables sensibles se manejan por entorno (`.env`, variables de entorno del sistema), nunca hardcodeadas.
- Ver `.gitignore` para archivos excluidos.

## 10. Alcance del proyecto (progresión)

| Módulo | Evaluación |
|---|---|
| Pedidos + Catálogo | **EP1 (actual)** |
| Notificaciones (RabbitMQ) | EP2 |
| Reportería y Auditoría (Kafka) | EP3 |

---

*DSY1107 — Desarrollo Cloud Native I — Primer Bloque Evaluativo (EP1/EP2)*