# Infraestructura Kafka — Pedidos360

Clúster de laboratorio con **3 ZooKeeper + 3 brokers Kafka + Kafka UI**, en modo
ZooKeeper (no KRaft), más los scripts para crear los dos tópicos del proyecto.

## 1. Requisitos

- Docker Engine / Docker Desktop con Compose v2.
- Al menos **4 GB de RAM** libres. Los heaps están limitados:
  - cada ZooKeeper: 128 MB inicial / 256 MB máximo
  - cada broker: 256 MB inicial / 512 MB máximo
  - Kafka UI: 128 MB inicial / 256 MB máximo

## 2. Arrancar el clúster

```bash
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml --env-file infra/.env up -d
docker compose -f infra/docker-compose.yml ps
```

Deben quedar arriba `pedidos360-zookeeper1..3`, `pedidos360-broker1..3` y
`pedidos360-kafka-ui`. Espera a que los brokers aparezcan como `healthy`.

## 3. Crear los tópicos

```bash
bash infra/scripts/crear-topicos.sh
```

En PowerShell:

```powershell
pwsh infra/scripts/crear-topicos.ps1
```

Los scripts son idempotentes (`--if-not-exists`) y al final muestran
`--describe` de cada tópico.

## 4. Verificar

```bash
# Listar tópicos
docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-topics --bootstrap-server broker1:29092 --list

# Detalle de un tópico (particiones, réplicas, ISR, configs)
docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-topics --bootstrap-server broker1:29092 --describe --topic pedidos.eventos

# Grupos y lag
docker compose -f infra/docker-compose.yml exec broker1 \
  kafka-consumer-groups --bootstrap-server broker1:29092 --describe --all-groups
```

Kafka UI: <http://localhost:8085>

## 5. Tópicos

| Tópico | Particiones | Réplicas | Retención | cleanup.policy | Key |
|---|---:|---:|---|---|---|
| `pedidos.eventos` | 3 | 3 | 7 días (`604800000` ms) | `delete` | `pedidoId` |
| `stock.eventos` | 3 | 3 | Infinita (`-1`) | `compact` | `productoId` |

Ambos con `min.insync.replicas=2`.

## 6. Acceso desde fuera de la EC2

`EXTERNAL_HOST` controla la dirección que los brokers anuncian a los clientes
externos:

- **Todo en la misma EC2** o en tu PC: `EXTERNAL_HOST=localhost`.
- **Postman/otra máquina apuntando a la EC2**: cambia `EXTERNAL_HOST` por la IP
  pública actual del Learner Lab y vuelve a levantar el compose. Abre en el
  Security Group los puertos **9092, 9093, 9094** y **8085**.

La IP pública del Learner Lab cambia al reiniciar: actualiza `infra/.env` y
ejecuta de nuevo `docker compose ... up -d`.

## 7. Despliegue completo en EC2

La guía paso a paso para levantar RabbitMQ, Kafka y los cinco microservicios con
systemd está en [`docs/despliegue-ec2.md`](../docs/despliegue-ec2.md).

## 8. Detener

```bash
docker compose -f infra/docker-compose.yml down
```

Para borrar también los datos: `docker compose -f infra/docker-compose.yml down -v`.
