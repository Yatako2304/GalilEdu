#!/usr/bin/env bash
# Actualiza GalilEdu en el servidor con lo último de main y reconstruye los contenedores.
# Lo ejecuta GitHub Actions (.github/workflows/deploy.yml) después de cada merge a main con el CI en verde.
# También se puede correr a mano en el servidor:  bash /opt/galiledu/deploy/actualizar.sh
set -euo pipefail

# Todo va dentro de una función: bash la lee completa antes de ejecutarla, así el "git reset"
# que actualiza este mismo archivo no lo cambia a mitad de camino.
principal() {
  DIR="${DIR:-/opt/galiledu}"
  RAMA="${RAMA:-main}"
  cd "$DIR"
  # La primera instalación corre con sudo: evita el aviso de Git "dubious ownership".
  git() { command git -c safe.directory="$DIR" "$@"; }

  echo "▶ Descargando $RAMA…"
  git fetch -q origin "$RAMA"
  git reset -q --hard "origin/$RAMA"
  echo "   Versión: $(git log -1 --format='%h %s')"

  echo "▶ Construyendo y levantando contenedores…"
  docker compose --env-file deploy/.env up -d --build --remove-orphans

  echo "▶ Esperando a que la aplicación responda…"
  for i in $(seq 1 60); do
    if curl -fsS -o /dev/null http://localhost/api/actuator/health; then
      echo "✅ GalilEdu está arriba ($(git log -1 --format='%h'))."
      docker image prune -f >/dev/null
      exit 0
    fi
    sleep 5
  done

  echo "❌ La aplicación no respondió en 5 minutos. Últimos logs del backend:"
  docker compose --env-file deploy/.env logs --tail=60 backend
  exit 1
}

principal "$@"
