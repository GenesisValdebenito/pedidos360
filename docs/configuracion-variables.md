# Configuración de variables sensibles

Este documento explica paso a paso cómo obtener y completar los valores reales que hoy están como ejemplos o plantillas en el proyecto.

## 1. Frontend Angular

Archivo objetivo:
- `frontend/src/environments/environment.ts`

### Paso 1: obtener el Client ID de Microsoft Entra ID
1. Ingresa a Azure Portal.
2. Abre Microsoft Entra ID.
3. Entra a App registrations.
4. Selecciona la aplicación registrada para el frontend.
5. Copia el valor de `Application (client) ID`.
6. Pega ese valor en:
   - `clientId: 'TU_CLIENT_ID'`

### Paso 2: obtener el tenant ID
1. En la misma App Registration, revisa la sección Overview.
2. Busca el valor de Directory (tenant) ID.
3. Pega ese valor en:
   - `authority: 'https://login.microsoftonline.com/TU_TENANT_ID'`

### Paso 3: definir la URI de redirección
1. En la App Registration, entra a Authentication.
2. Configura `http://localhost:4200` como Redirect URI para el frontend local.
3. Pega ese valor en:
   - `redirectUri: 'http://localhost:4200'`

### Paso 4: obtener el scope de la API
1. Entra a la App Registration de la API backend.
2. Busca Expose an API.
3. Copia el Application ID URI o el scope público.
4. Pega el valor en:
   - `apiScope: 'api://TU_API_APP_ID/Pedidos.Read'`

### Paso 5: obtener la URL del API Gateway
1. En AWS Console, entra a API Gateway.
2. Abre la API creada para Pedidos360.
3. Copia la URL base del endpoint.
4. Pega ese valor en:
   - `apiUrl: 'https://TU_API_GATEWAY_URL.execute-api.us-east-1.amazonaws.com'`

### Resultado final esperado
```ts
export const environment = {
  production: false,
  clientId: 'CLIENT_ID_REAL',
  authority: 'https://login.microsoftonline.com/TENANT_ID_REAL',
  redirectUri: 'http://localhost:4200',
  apiScope: 'api://API_APP_ID_REAL/Pedidos.Read',
  apiUrl: 'https://TU_API_GATEWAY_URL.execute-api.us-east-1.amazonaws.com'
};
```

---

## 2. Backend orders-service

Archivo objetivo:
- `backend/orders-service/src/main/resources/application.properties`

### Paso 1: obtener el issuer URI de Microsoft Entra ID
1. En Azure Portal, entra a la App Registration de la API.
2. En Overview, identifica el issuer URI o tenant.
3. Completa:
   - `spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_TENANT_ISSUER_URI}`

### Paso 2: obtener el audience/client ID de la API
1. En la App Registration de la API, revisa Application (client) ID.
2. Pega el valor en:
   - `spring.security.oauth2.resourceserver.jwt.audiences[0]=${AZURE_API_CLIENT_ID}`

### Paso 3: obtener la conexión a PostgreSQL (Supabase)
1. En Supabase, entra a Project Settings > Database.
2. Copia:
   - Host
   - Port
   - Database name
   - User
   - Password
3. Completa las variables:
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USERNAME`
   - `DB_PASSWORD`

### Resultado final esperado
```properties
spring.application.name=orders-service
server.port=8081
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_TENANT_ISSUER_URI}
spring.security.oauth2.resourceserver.jwt.audiences[0]=${AZURE_API_CLIENT_ID}

spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?prepareThreshold=0
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## 3. Backend catalog-service

Archivo objetivo:
- `backend/catalog-service/src/main/resources/application.properties`

Sigue exactamente los mismos pasos que en orders-service, con los valores reales de la API y la base de datos.

---

## 4. Plantilla para completar manualmente

Si quieres rellenar los valores reales a mano sin tocar el código, usa esta plantilla:

- [docs/plantilla-credenciales-locales.md](plantilla-credenciales-locales.md)

Es un formulario en blanco para completar tus datos reales localmente y mantenerlos fuera del repositorio.

## 5. Recomendación de uso seguro

No copies valores reales directamente al repo. Lo correcto es:

- usar variables de entorno del sistema
- o crear un archivo local no versionado con tus valores reales
- dejar en Git solo templates y ejemplos

Ejemplo de variables de entorno en Windows PowerShell:
```powershell
$env:AZURE_TENANT_ISSUER_URI="https://login.microsoftonline.com/TU_TENANT_ID/v2.0"
$env:AZURE_API_CLIENT_ID="TU_CLIENT_ID_API"
$env:DB_HOST="aws-0-us-east-2.pooler.supabase.com"
$env:DB_PORT="6543"
$env:DB_NAME="postgres"
$env:DB_USERNAME="postgres.TU_USUARIO"
$env:DB_PASSWORD="TU_PASSWORD_REAL"
```

Luego el proyecto puede leer esas variables sin guardar secretos en el código.

---

## 6. Checklist final

Antes de ejecutar la app, valida esto:

- [ ] `frontend/src/environments/environment.ts` tiene valores reales o variables de entorno
- [ ] `backend/orders-service/src/main/resources/application.properties` tiene solo valores reales locales
- [ ] `backend/catalog-service/src/main/resources/application.properties` tiene solo valores reales locales
- [ ] `application.properties` y `environment.ts` no están en Git como archivos sensibles
- [ ] Se mantiene el patrón de templates en archivos `example` o `template`

> No subir credenciales reales ni configuraciones sensibles al repositorio. Mantener siempre el patrón: ejemplo seguro en Git, valores reales solo en entorno local.
