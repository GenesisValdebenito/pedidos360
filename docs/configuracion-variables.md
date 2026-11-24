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

### Paso 4: configurar la credencial interna entre servicios
1. Genera un valor secreto largo y aleatorio para `PEDIDOS360_INTERNAL_TOKEN`.
2. Configura el mismo valor en el entorno de `orders-service` y `catalog-service`.
3. No lo incluyas en Git ni lo uses en Angular. El endpoint de descuento de stock lo valida para aceptar llamadas internas.

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
pedidos360.catalog.internal-token=${PEDIDOS360_INTERNAL_TOKEN}

# Kafka (mismos valores para orders, catalog, reporting y kafka-admin)
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093,localhost:9094}
spring.kafka.client-id=orders-service
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.properties.spring.json.add.type.headers=false
```

---

## 3. Backend catalog-service

Archivo objetivo:
- `backend/catalog-service/src/main/resources/application.properties`

Sigue exactamente los mismos pasos que en orders-service, con los valores reales de la API y la base de datos. Añade también:

```properties
pedidos360.internal-token=${PEDIDOS360_INTERNAL_TOKEN}

spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093,localhost:9094}
spring.kafka.client-id=catalog-service
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.properties.spring.json.add.type.headers=false
```

---

## 4. Kafka: infraestructura y microservicios

### Paso 1: configurar `infra/.env`
1. Copia `infra/.env.example` a `infra/.env` (este archivo no se versiona).
2. Define `EXTERNAL_HOST`:
   - `localhost` si los microservicios y Kafka están en la misma máquina o EC2.
   - la IP pública de la EC2 si Postman u otra máquina se conecta desde fuera.
3. Deja `KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094` cuando los microservicios corren en la misma instancia que los brokers.
4. `KAFKA_UI_PORT` es el puerto de la interfaz web (por defecto `8085`).

```properties
EXTERNAL_HOST=localhost
KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094
KAFKA_UI_PORT=8085
```

Si cambia la IP pública de la EC2, actualiza `EXTERNAL_HOST` y vuelve a levantar el compose. Abre en el Security Group los puertos **9092, 9093 y 9094** solo si un cliente externo los necesita.

### Paso 2: crear los tópicos
Con los brokers saludables:

```bash
bash infra/scripts/crear-topicos.sh
```

o en PowerShell:

```powershell
pwsh infra/scripts/crear-topicos.ps1
```

### Paso 3: configurar `reporting-service`
Copia `backend/reporting-service/src/main/resources/application-example.properties` a `application.properties` y completa `AZURE_TENANT_ISSUER_URI`, `AZURE_API_CLIENT_ID` y, si corresponde, `KAFKA_BOOTSTRAP_SERVERS`.

### Paso 4: configurar `kafka-admin-service`
Copia `backend/kafka-admin-service/src/main/resources/application-example.properties` a `application.properties` y completa las mismas variables de Entra ID y Kafka.

> Kafka no requiere usuario ni contraseña en este laboratorio (`PLAINTEXT` dentro de la red local). En un entorno real se usaría SASL/SCRAM o mTLS; esas credenciales también deberían vivir fuera de Git.

---

## 5. Plantilla para completar manualmente

Si quieres rellenar los valores reales a mano sin tocar el código, usa esta plantilla:

- [docs/plantilla-credenciales-locales.md](plantilla-credenciales-locales.md)

Es un formulario en blanco para completar tus datos reales localmente y mantenerlos fuera del repositorio.

## 6. Recomendación de uso seguro

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
$env:PEDIDOS360_INTERNAL_TOKEN="UN_SECRETO_LARGO_Y_ALEATORIO_COMPARTIDO"
```

Luego el proyecto puede leer esas variables sin guardar secretos en el código.

### Configurar RabbitMQ
Para desarrollo local, levanta el broker desde la raíz del repositorio con `docker compose up -d`. El archivo `docker-compose.yml` usa credenciales locales de desarrollo y publica los puertos solo en `127.0.0.1`.

Si necesitas cambiar las credenciales locales, define `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD` antes de iniciar Docker Compose y configura los mismos valores para los servicios Spring Boot.

Para ejecutar `orders-service`, agrega las siguientes propiedades a su `application.properties` local existente; no reemplaces la configuración de Entra ID ni de Supabase. Para `notifications-service`, copia su `application-example.properties` como `application.properties`. RabbitMQ se configura mediante:

- `RABBITMQ_HOST` (local: `localhost`; en EC2 si RabbitMQ está en la misma instancia: `localhost`)
- `RABBITMQ_PORT` (local: `5672`)
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`

La consola de administración local se encuentra en `http://localhost:15672`. No expongas los puertos de RabbitMQ a Internet; en EC2 el broker debe permanecer accesible solo desde la instancia. El envío se realiza después del commit del pedido; si RabbitMQ no está disponible, el pedido se conserva y queda un error explícito en los logs, pero la notificación no se encola.

La configuración local usa el mismo usuario y contraseña de desarrollo que el `docker-compose.yml`. En EC2 define credenciales propias mediante variables de entorno; no reutilices las credenciales locales.

---

## 7. Checklist final

Antes de ejecutar la app, valida esto:

- [ ] `frontend/src/environments/environment.ts` tiene valores reales o variables de entorno
- [ ] `backend/orders-service/src/main/resources/application.properties` tiene solo valores reales locales
- [ ] `backend/catalog-service/src/main/resources/application.properties` tiene solo valores reales locales
- [ ] `backend/reporting-service/src/main/resources/application.properties` está configurado y no se versiona
- [ ] `backend/kafka-admin-service/src/main/resources/application.properties` está configurado y no se versiona
- [ ] `infra/.env` existe con `EXTERNAL_HOST` correcto y no se versiona
- [ ] Los tópicos `pedidos.eventos` y `stock.eventos` fueron creados con el script
- [ ] `application.properties` y `environment.ts` no están en Git como archivos sensibles
- [ ] Se mantiene el patrón de templates en archivos `example` o `template`

> No subir credenciales reales ni configuraciones sensibles al repositorio. Mantener siempre el patrón: ejemplo seguro en Git, valores reales solo en entorno local.
