# Despliegue en AWS EC2 — Pedidos360 EP3

Guía para ejecutar Pedidos360 completo en **una instancia EC2**: RabbitMQ,
clúster Kafka de 3 brokers, los cinco microservicios Spring Boot y la
integración con API Gateway.

> Los secretos se escriben solo en archivos fuera del repositorio. Esta guía no
> contiene credenciales reales y ningún comando las imprime.

---

## 1. Arquitectura del despliegue

```text
Internet
   │
   ▼
AWS API Gateway (JWT + CORS)
   │
   ▼
EC2  ────────────────────────────────────────────────┐
│  orders-service      :8081                          │
│  catalog-service     :8082                          │
│  reporting-service   :8083                          │
│  kafka-admin-service :8084                          │
│  notifications-service (sin puerto HTTP público)    │
│                                                     │
│  Docker: RabbitMQ :5672                             │
│  Docker: 3 ZooKeeper + 3 brokers Kafka :9092-9094   │
│  Docker: Kafka UI :8085 (opcional)                  │
└─────────────────────────────────────────────────────┘
   │
   ▼
PostgreSQL cloud (Supabase u otro proveedor)
```

Los microservicios corren como unidades **systemd** (`pedidos360@<servicio>`).
Así se reinician si fallan y sobreviven al cierre de la sesión SSH.

---

## 2. Requisitos de la instancia

| Recurso | Recomendado | Motivo |
|---|---|---|
| Tipo | `t3.large` (2 vCPU, 8 GB) | Kafka usa ~2 GB y los cinco servicios Java ~2 GB |
| Disco | 30 GB gp3 | Imágenes Docker, Maven y jars |
| SO | Amazon Linux 2023 | Paquetes disponibles en el laboratorio |
| IP | Pública o Elastic IP | API Gateway y Postman necesitan alcanzarla |

Con `t3.medium` (4 GB) el sistema puede quedar sin memoria al compilar y
ejecutar todo junto. Si es la única opción, compile antes de levantar Kafka y
limite los heaps.

### Security Group

| Puerto | Origen | Uso |
|---:|---|---|
| 22 | Tu IP | SSH |
| 8081-8084 | Tu IP o API Gateway | Microservicios |
| 8085 | Tu IP | Kafka UI (opcional) |
| 9092-9094 | Tu IP | Kafka externo (opcional) |
| 5672, 15672 | Nadie | RabbitMQ queda solo dentro de la instancia |

Si todos los clientes están en la misma EC2, **no abras 9092-9094**. El
`EXTERNAL_HOST=localhost` evita anunciar la IP pública. Si Postman se conecta
desde tu PC, abre esos puertos solo para tu IP y actualiza `EXTERNAL_HOST`.

> Para la demo académica se puede abrir 8081-8084 a la IP del estudiante y
> probar con Postman. No se recomienda abrirlos a `0.0.0.0/0`: los servicios
> validan JWT, pero la superficie de ataque aumenta.

---

## 3. Preparar el sistema operativo

Conéctate por SSH:

```bash
ssh -i llave.pem ec2-user@IP_PUBLICA
```

Instala Docker, Git, Java 17 y Maven:

```bash
sudo dnf update -y
sudo dnf install -y docker git java-17-amazon-corretto-devel maven
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

Cierra la sesión y vuelve a entrar para que el grupo `docker` se aplique:

```bash
exit
# volver a conectar por SSH
docker version
java -version
mvn -version
```

Verifica Docker Compose v2:

```bash
docker compose version
```

Si el comando no existe, instala el plugin:

```bash
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL \
  https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version
```

**Resultado esperado:** `java -version` muestra 17, `mvn -version` usa Java 17
y `docker compose version` responde con v2.x.

---

## 4. Clonar el repositorio

```bash
cd /home/ec2-user
git clone https://github.com/GenesisValdebenito/pedidos360.git
cd pedidos360
```

Si el repositorio es privado, usa un token de GitHub solo para el clonado y no
lo guardes en el remoto (`git remote set-url` sin credenciales después).

---

## 5. Configurar secretos fuera del repositorio

Crea el archivo de entorno que leerán las unidades systemd:

```bash
mkdir -p /home/ec2-user/pedidos360-config
cp infra/ec2/pedidos360.env.example \
   /home/ec2-user/pedidos360-config/pedidos360.env
chmod 600 /home/ec2-user/pedidos360-config/pedidos360.env
nano /home/ec2-user/pedidos360-config/pedidos360.env
```

Completa:

- `AZURE_TENANT_ISSUER_URI` y `AZURE_API_CLIENT_ID` de Entra ID.
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- `PEDIDOS360_INTERNAL_TOKEN`: valor largo y aleatorio, distinto al local.
- `RABBITMQ_USERNAME` y `RABBITMQ_PASSWORD`: los mismos del paso 6.
- `KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094`.
- `PEDIDOS360_CATALOG_URL=http://localhost:8082`.

Comprueba que el archivo no quedó dentro de Git:

```bash
git status --short
```

**Resultado esperado:** no aparece `pedidos360.env` porque está fuera del
repositorio.

---

## 6. Levantar RabbitMQ

El `docker-compose.yml` de la raíz lee credenciales desde un `.env` local.
Créalo en la raíz del repositorio (está ignorado por Git):

```bash
cat > .env <<'EOF'
RABBITMQ_USERNAME=pedidos360
RABBITMQ_PASSWORD=CAMBIA_ESTE_VALOR
EOF
chmod 600 .env
docker compose up -d
docker compose ps
```

**Resultado esperado:** `pedidos360-rabbitmq` en estado `healthy`. La consola
`15672` no se expone fuera de la instancia.

> Las credenciales de `.env` deben coincidir con `RABBITMQ_USERNAME` y
> `RABBITMQ_PASSWORD` del archivo de systemd.

---

## 7. Levantar Kafka y crear los tópicos

```bash
cp infra/.env.example infra/.env
nano infra/.env
```

Con todos los servicios en la misma EC2:

```properties
EXTERNAL_HOST=localhost
KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094
KAFKA_UI_PORT=8085
```

Levanta el clúster:

```bash
docker compose -f infra/docker-compose.yml --env-file infra/.env up -d
docker compose -f infra/docker-compose.yml ps
```

Espera a que los tres brokers estén `healthy` y crea los tópicos:

```bash
bash infra/scripts/crear-topicos.sh
```

Verifica:

```bash
docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-topics --bootstrap-server broker1:29092 --describe --topic pedidos.eventos

docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-topics --bootstrap-server broker1:29092 --describe --topic stock.eventos
```

**Resultado esperado:** ambos tópicos con 3 particiones, factor de réplica 3 e
ISR 3. `stock.eventos` muestra `cleanup.policy=compact`.

---

## 8. Compilar e instalar los microservicios

El script crea los `application.properties` a partir de los ejemplos, compila
cada módulo, copia los jars a `/home/ec2-user/pedidos360-app` e instala la
unidad systemd:

```bash
bash infra/scripts/desplegar-ec2.sh --instalar
```

El script:

1. Exige que exista `/home/ec2-user/pedidos360-config/pedidos360.env`.
2. Ejecuta `mvn -q -DskipTests package` por servicio.
3. Copia `orders-service`, `catalog-service`, `notifications-service`,
   `reporting-service` y `kafka-admin-service` como `<servicio>.jar`.
4. Instala `infra/systemd/pedidos360@.service` y reinicia cada instancia.

Verifica:

```bash
systemctl --no-pager --lines=0 status \
  pedidos360@orders-service \
  pedidos360@catalog-service \
  pedidos360@notifications-service \
  pedidos360@reporting-service \
  pedidos360@kafka-admin-service

ss -ltnp | grep -E ':(8081|8082|8083|8084)'
```

**Resultado esperado:** los cinco servicios `active (running)` y los puertos
8081-8084 escuchando.

Si algún servicio falla:

```bash
journalctl -u pedidos360@reporting-service -n 80 --no-pager
```

---

## 9. Verificar el flujo completo

### 9.1 Infraestructura

```bash
docker compose ps
docker compose -f infra/docker-compose.yml ps
```

Deben aparecer RabbitMQ `healthy`, tres ZooKeeper, tres brokers `healthy` y
Kafka UI.

### 9.2 Tópicos y grupos

```bash
docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-consumer-groups --bootstrap-server broker1:29092 --describe --all-groups
```

Después de crear un pedido deben existir `reporteria-group` y
`auditoria-group`, con lag cercano a 0.

### 9.3 API con token

Obtén un token desde el frontend o Postman y expórtalo solo en tu sesión:

```bash
export TOKEN="eyJ..."
```

Crea un pedido con un producto existente y consulta los reportes:

```bash
curl -s http://localhost:8081/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productoId":1,"cantidad":2}'

# Acepta el pedido para generar también el evento de stock
curl -s -X PUT http://localhost:8081/api/orders/1/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"ACEPTADO"}'

curl -s http://localhost:8083/api/reportes/kpis \
  -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8083/api/reportes/auditoria?limite=5 \
  -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8084/api/kafka/grupos \
  -H "Authorization: Bearer $TOKEN"
```

> Ajusta `productoId` a un producto con stock y el `id` del pedido al que
> devolvió el primer comando.

**Resultado esperado:** el pedido se crea, los KPIs aumentan, la auditoría
muestra el evento y `kafka-admin` lista los dos grupos con su lag.

### 9.4 Kafka UI

Abre `http://IP_PUBLICA:8085` solo si habilitaste el puerto. Deben verse los dos
tópicos con sus particiones y mensajes.

---

## 10. API Gateway y frontend

### 10.1 Rutas nuevas

En la API HTTP existente agrega las rutas de los servicios nuevos:

| Ruta | Integración |
|---|---|
| `ANY /api/reportes/{proxy+}` | `http://IP_EC2:8083/api/reportes/{proxy}` |
| `ANY /api/kafka/{proxy+}` | `http://IP_EC2:8084/api/kafka/{proxy}` |

Las rutas de pedidos y catálogo se mantienen igual. Asocia el mismo
**JWT Authorizer** (issuer y audience de Entra ID) a las rutas nuevas.

### 10.2 CORS

En la configuración CORS de API Gateway permite el origen del frontend
(`http://localhost:4200` en local o el dominio desplegado) y los encabezados
`Authorization` y `Content-Type`.

### 10.3 Frontend

Si el frontend ya está desplegado, solo actualiza `apiUrl` para que apunte al
API Gateway. Si se publica desde la EC2, compila con:

```bash
cd frontend
npm ci
npm run build
```

y sirve `dist/` con nginx o súbelo a S3/CloudFront. No publiques
`environment.ts` con secretos: solo contiene URLs y IDs públicos.

---

## 11. Operación

### Ver logs

```bash
journalctl -u pedidos360@orders-service -f
journalctl -u pedidos360@reporting-service -f
```

### Reiniciar un servicio

```bash
sudo systemctl restart pedidos360@reporting-service
```

### Actualizar una nueva versión

```bash
cd /home/ec2-user/pedidos360
git pull
bash infra/scripts/desplegar-ec2.sh --instalar
```

### Detener los microservicios

```bash
for s in orders-service catalog-service notifications-service \
         reporting-service kafka-admin-service; do
  sudo systemctl stop "pedidos360@$s"
done
```

### Detener la infraestructura

```bash
docker compose -f infra/docker-compose.yml down
docker compose down
```

No uses `down -v` salvo que quieras borrar los datos de Kafka y RabbitMQ.

---

## 12. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `401 Unauthorized` | Issuer o audience incorrectos | Revisa `AZURE_TENANT_ISSUER_URI` y `AZURE_API_CLIENT_ID`; el token debe ser de la misma app |
| `403 Forbidden` | Falta el rol Admin/Operador | Revisa el claim `roles` del token |
| Reportes vacíos | Tópicos no creados o bootstrap incorrecto | Ejecuta el script de tópicos y revisa `KAFKA_BOOTSTRAP_SERVERS` |
| `reporting-service` no arranca | Kafka aún no está saludable | Espera los brokers y reinicia el servicio |
| Kafka no resuelve desde Postman | `EXTERNAL_HOST` con `localhost` | Cambia a la IP pública, recrea el compose y abre 9092-9094 |
| `OutOfMemoryError` | Instancia pequeña | Usa `t3.large`, revisa `free -h` y `docker stats` |
| Error de conexión a PostgreSQL | Credenciales o host incorrectos | Prueba el pooler de Supabase y mantén `prepareThreshold=0` |
| Puerto ocupado | Otro proceso en 8081-8084 | `ss -ltnp` y detén el proceso que use el puerto |

---

## 13. Checklist de entrega

- [ ] Instancia `t3.large` con 30 GB y Security Group mínimo.
- [ ] Docker, Java 17 y Maven instalados.
- [ ] Repositorio clonado en `/home/ec2-user/pedidos360`.
- [ ] `pedidos360.env` fuera del repositorio y con permisos `600`.
- [ ] RabbitMQ `healthy` y credenciales coincidentes con el env.
- [ ] Kafka con 3 brokers `healthy` y tópicos con RF 3.
- [ ] Cinco unidades `pedidos360@...` activas.
- [ ] Puertos 8081-8084 escuchando.
- [ ] Rutas de API Gateway para reportes y Kafka con JWT.
- [ ] Flujo pedido → Kafka → KPIs/auditoría verificado.
- [ ] `GET /api/kafka/grupos` muestra lag 0.
- [ ] Sin secretos en `git status`.

---

## 14. Evidencia para la defensa

1. `docker compose -f infra/docker-compose.yml ps`: clúster replicado.
2. `systemctl status 'pedidos360@*'`: servicios administrados por systemd.
3. `kafka-topics --describe`: 3 particiones, RF 3, ISR 3.
4. `kafka-consumer-groups --describe`: dos grupos y lag 0.
5. `GET /api/reportes/kpis` y `/api/reportes/auditoria`: datos reales.
6. `GET /api/kafka/grupos`: administración y observabilidad.
7. `git status`: secretos fuera del repositorio.
