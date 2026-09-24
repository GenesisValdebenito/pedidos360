# Plantilla local de variables sensibles

Completa esta plantilla con tus valores reales en tu equipo local. No la subas a Git ni la compartas en el repositorio.

---

## 1. Microsoft Entra ID / Azure

### Frontend app
- Nombre de la app: __________________________
- Client ID (Frontend): ______________________
- Tenant ID: ________________________________
- Authority URL: https://login.microsoftonline.com/__________________
- Redirect URI local: _______________________
- Scope de la API: api://______________________/Pedidos.Read

### API backend
- Nombre de la API: ________________________
- Client ID de la API: ______________________
- Application ID URI: api://____________________
- Issuer URI: https://login.microsoftonline.com/__________________/v2.0
- Audience / API client ID: __________________

---

## 2. AWS API Gateway

- URL base del API Gateway: https://______________________.execute-api.us-east-1.amazonaws.com
- Stage: _________________________________
- Ruta principal de pedidos: __________________
- Ruta principal de catálogo: ________________

---

## 3. Base de datos (Supabase / PostgreSQL)

- Host: _________________________________
- Port: _________________________________
- Database name: ________________________
- Username: _____________________________
- Password: _____________________________
- JDBC URL completa: jdbc:postgresql://____________________

---

## 4. Variables de entorno locales

Copiar y completar en tu entorno local (PowerShell, .env, o archivo local no versionado):

```powershell
$env:AZURE_TENANT_ISSUER_URI="https://login.microsoftonline.com/__________________/v2.0"
$env:AZURE_API_CLIENT_ID="____________________"
$env:DB_HOST="____________________"
$env:DB_PORT="____________________"
$env:DB_NAME="____________________"
$env:DB_USERNAME="____________________"
$env:DB_PASSWORD="____________________"
```

---

## 5. Frontend environment.ts final

```ts
export const environment = {
  production: false,
  clientId: '____________________',
  authority: 'https://login.microsoftonline.com/____________________',
  redirectUri: 'http://localhost:4200',
  apiScope: 'api://____________________/Pedidos.Read',
  apiUrl: 'https://____________________.execute-api.us-east-1.amazonaws.com'
};
```

---

## 6. Backend application.properties final

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

## 7. Checklist final

- [ ] Tengo los valores reales de Azure y del tenant
- [ ] Tengo la URL real del API Gateway
- [ ] Tengo la conexión real a la base de datos
- [ ] No he dejado valores reales en el repositorio
- [ ] Todos los valores sensibles están solo en un archivo local o en variables de entorno
