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
- **Backend:** Microservicios en Spring Boot (Pedidos y Catálogo), desplegados en AWS EC2
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
  ↓
BASE DE DATOS CLOUD
```

## 3. Estructura del repositorio

```
pedidos360-dsy1107/
├── frontend/              # Angular + MSAL
├── backend/
│   ├── orders-service/    # Gestión de Pedidos
│   └── catalog-service/   # Catálogo / Stock
├── docs/                  # Diagramas y capturas de arquitectura
├── .gitignore
└── README.md
```

## 4. Módulos funcionales

### 4.1 Gestión de Pedidos
- Crear, listar y consultar pedidos.
- Cambiar estado: `CREADO → ACEPTADO → EN_PREPARACIÓN → DESPACHADO → ENTREGADO` (o `CANCELADO`).
- Regla de negocio: un pedido **no puede pasar a DESPACHADO** sin haber pasado antes por **ACEPTADO**.

### 4.2 Catálogo de Productos / Stock
- Listar productos disponibles.
- Crear y editar productos (solo Admin).
- Regla de negocio: al **aceptar** un pedido, el **stock disminuye** según los productos del pedido.

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

### Frontend (Angular)
```bash
cd frontend
npm install
ng serve
```
Configurar variables de entorno de MSAL en `src/environments/environment.ts` (clientId, authority, redirectUri) — **no subir valores reales al repo**, usar un archivo de ejemplo `environment.example.ts`.

### Backend (Spring Boot)
```bash
cd backend/orders-service
./mvnw spring-boot:run

cd backend/catalog-service
./mvnw spring-boot:run
```
Configurar `application.properties` con las credenciales de BD cloud vía variables de entorno — **no subir credenciales reales al repo**.

## 8. Backlog / Progreso del proyecto

### Sesión 1 — Identidad + esqueleto
- [ ] Repositorio y estructura de carpetas creada
- [ ] `.gitignore` configurado
- [ ] App registrada en Microsoft Entra ID (tenant, App Registration, Redirect URI)
- [ ] Usuarios de prueba con roles (Admin/Operador/Cliente)
- [ ] Angular + MSAL: login/logout funcionando
- [ ] Scaffolding de orders-service y catalog-service (compilando)

### Sesión 2 — Backend + seguridad + persistencia
- [ ] BD cloud configurada y conectada
- [ ] Entidades y repositorios: Order, OrderItem, Product
- [ ] Spring Security valida JWT (issuer, audience, firma, expiración)
- [ ] Endpoints de Pedidos (CRUD + cambio de estado + regla ACEPTADO→DESPACHADO)
- [ ] Endpoints de Catálogo (CRUD + descuento de stock al aceptar pedido)
- [ ] API Gateway desplegado con JWT Authorizer + CORS

### Sesión 3 — Frontend + integración + pruebas
- [ ] Pantallas `/dashboard`, `/orders`, `/catalog` conectadas a la API
- [ ] MsalGuard en rutas protegidas + MsalInterceptor adjuntando token
- [ ] Diferenciación por rol funcionando
- [ ] Flujo end-to-end probado (Angular → Gateway → Backend → BD)
- [ ] Casos 200/401/403 probados y documentados
- [ ] Repositorio limpio, sin secretos
- [ ] Recorrido del video EP2 preparado

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