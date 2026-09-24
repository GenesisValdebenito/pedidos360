# Pruebas de Seguridad — Pedidos360

## 1. Autenticación y Autorización JWT

| Caso | Resultado esperado | Resultado validado |
| --- | --- | --- |
| Endpoint protegido con token válido y permiso correcto | 200 OK | Confirmado |
| Endpoint protegido sin token | 401 Unauthorized | Confirmado |
| Endpoint protegido con token inválido | 401 Unauthorized | Confirmado |
| Token válido pero sin rol requerido | 403 Forbidden | Confirmado (Cliente intentando POST /api/catalog/products, requiere Admin) |

## 2. Filtrado por rol

| Escenario | Resultado validado |
| --- | --- |
| Usuario Cliente en GET /api/orders | Solo ve sus propios pedidos |
| Usuario Admin/Operador en GET /api/orders | Ve todos los pedidos |

## 3. Reglas de negocio

| Caso | Resultado esperado | Resultado validado |
| --- | --- | --- |
| PUT /api/orders/{id}/status a DESPACHADO sin pasar antes por ACEPTADO | 400 Bad Request | Confirmado |
| PUT /api/orders/{id}/status a ACEPTADO | 200 OK | Confirmado, con descuento automático de stock en catalog-service |
| Comunicación entre microservicios | HTTP segura con propagación de token | Confirmado |

## 4. Flujo End-to-End validado

| Paso | Validación |
| --- | --- |
| Angular (login MSAL) | Confirmado |
| API Gateway (JWT Authorizer) | Confirmado |
| EC2 (Spring Security) | Confirmado |
| Supabase (persistencia) | Confirmado |
| Usuarios de prueba | Admin, Operador, Cliente | 

## 5. Endpoints públicos

| Endpoint | Resultado validado |
| --- | --- |
| GET /api/publico | 200 OK sin autenticación |
| GET /api/catalog/publico | 200 OK sin autenticación |

Todas las pruebas fueron ejecutadas y confirmadas manualmente con Postman y curl, además de demostradas en vivo en el video EP2. Fecha de validación: 23-24 de septiembre de 2026.
