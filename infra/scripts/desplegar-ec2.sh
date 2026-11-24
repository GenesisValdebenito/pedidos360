#!/usr/bin/env bash
# Compila los microservicios, copia los jars fuera del repositorio y,
# opcionalmente, instala y reinicia las unidades systemd.
#
# Uso:
#   bash infra/scripts/desplegar-ec2.sh
#   bash infra/scripts/desplegar-ec2.sh --instalar
#
# Requisitos: Java 17, Maven, git y el archivo de entorno con los secretos.

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DESTINO_JAR="${DESTINO_JAR:-/home/ec2-user/pedidos360-app}"
ARCHIVO_ENTORNO="${ARCHIVO_ENTORNO:-/home/ec2-user/pedidos360-config/pedidos360.env}"
UNIDAD_SISTEMA="$RAIZ/infra/systemd/pedidos360@.service"

SERVICIOS=(
  orders-service
  catalog-service
  notifications-service
  reporting-service
  kafka-admin-service
)

if [[ ! -f "$ARCHIVO_ENTORNO" ]]; then
  echo "ERROR: falta $ARCHIVO_ENTORNO" >&2
  echo "Copia infra/ec2/pedidos360.env.example y completa los valores reales." >&2
  exit 1
fi

mkdir -p "$DESTINO_JAR"

for servicio in "${SERVICIOS[@]}"; do
  modulo="$RAIZ/backend/$servicio"
  propiedades="$modulo/src/main/resources/application.properties"

  # El archivo de ejemplo no contiene secretos: los valores reales llegan por
  # variables de entorno desde systemd. Se regenera en cada despliegue para no
  # arrastrar propiedades antiguas.
  cp "$modulo/src/main/resources/application-example.properties" "$propiedades"

  echo "==> Compilando $servicio"
  (cd "$modulo" && mvn -q -DskipTests package)

  mapfile -t jars < <(find "$modulo/target" -maxdepth 1 \
    -name '*.jar' ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print)
  if [[ "${#jars[@]}" -eq 0 ]]; then
    echo "ERROR: no se generó ningún jar para $servicio" >&2
    exit 1
  fi

  cp "${jars[0]}" "$DESTINO_JAR/$servicio.jar"
  echo "    copiado a $DESTINO_JAR/$servicio.jar"
done

echo
echo "Jars listos. Para instalar y reiniciar los servicios:"
echo "  sudo bash $0 --instalar"

if [[ "${1:-}" != "--instalar" ]]; then
  exit 0
fi

echo "==> Instalando unidad systemd"
sudo install -m 644 "$UNIDAD_SISTEMA" /etc/systemd/system/pedidos360@.service
sudo systemctl daemon-reload

for servicio in "${SERVICIOS[@]}"; do
  sudo systemctl enable "pedidos360@$servicio"
done

for servicio in "${SERVICIOS[@]}"; do
  echo "==> Reiniciando pedidos360@$servicio"
  sudo systemctl restart "pedidos360@$servicio"
done

echo
echo "Estado:"
systemctl --no-pager --lines=0 status "${SERVICIOS[@]/#/pedidos360@}" || true
