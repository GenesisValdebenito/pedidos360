#!/usr/bin/env bash
# Crea los dos tópicos de Pedidos360 sobre el clúster de 3 brokers.
# Uso: bash infra/scripts/crear-topicos.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${SCRIPT_DIR}/../docker-compose.yml}"

kafka_topics() {
  docker compose -f "$COMPOSE_FILE" exec -T broker1 kafka-topics "$@"
}

echo "Creando pedidos.eventos (3 particiones, RF 3, retención 7 días)..."
kafka_topics \
  --bootstrap-server broker1:29092 \
  --create --if-not-exists \
  --topic pedidos.eventos \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --config min.insync.replicas=2

echo "Creando stock.eventos (3 particiones, RF 3, compactado)..."
kafka_topics \
  --bootstrap-server broker1:29092 \
  --create --if-not-exists \
  --topic stock.eventos \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=-1 \
  --config cleanup.policy=compact \
  --config min.insync.replicas=2

echo ""
echo "Tópicos creados. Detalle:"
kafka_topics --bootstrap-server broker1:29092 --describe --topic pedidos.eventos
kafka_topics --bootstrap-server broker1:29092 --describe --topic stock.eventos
