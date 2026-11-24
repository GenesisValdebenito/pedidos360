# Crea los dos tópicos de Pedidos360 sobre el clúster de 3 brokers.
# Uso: pwsh infra/scripts/crear-topicos.ps1
$ErrorActionPreference = "Stop"

$ComposeFile = Join-Path $PSScriptRoot "..\docker-compose.yml"

function Invoke-KafkaTopics {
    param([string[]]$KafkaArgs)

    & docker compose -f $ComposeFile exec -T broker1 kafka-topics $KafkaArgs
    if ($LASTEXITCODE -ne 0) {
        throw "kafka-topics falló con código $LASTEXITCODE"
    }
}

Write-Host "Creando pedidos.eventos (3 particiones, RF 3, retención 7 días)..."
Invoke-KafkaTopics -KafkaArgs @(
    "--bootstrap-server", "broker1:29092",
    "--create", "--if-not-exists",
    "--topic", "pedidos.eventos",
    "--partitions", "3",
    "--replication-factor", "3",
    "--config", "retention.ms=604800000",
    "--config", "cleanup.policy=delete",
    "--config", "min.insync.replicas=2"
)

Write-Host "Creando stock.eventos (3 particiones, RF 3, compactado)..."
Invoke-KafkaTopics -KafkaArgs @(
    "--bootstrap-server", "broker1:29092",
    "--create", "--if-not-exists",
    "--topic", "stock.eventos",
    "--partitions", "3",
    "--replication-factor", "3",
    "--config", "retention.ms=-1",
    "--config", "cleanup.policy=compact",
    "--config", "min.insync.replicas=2"
)

Write-Host ""
Write-Host "Tópicos creados. Detalle:"
Invoke-KafkaTopics -KafkaArgs @("--bootstrap-server", "broker1:29092", "--describe", "--topic", "pedidos.eventos")
Invoke-KafkaTopics -KafkaArgs @("--bootstrap-server", "broker1:29092", "--describe", "--topic", "stock.eventos")
