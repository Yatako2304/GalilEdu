#!/usr/bin/env bash
# Instala GalilEdu en un servidor Ubuntu nuevo (p. ej. AWS EC2). Se ejecuta UNA sola vez:
#
#   curl -fsSL https://raw.githubusercontent.com/Yatako2304/GalilEdu/main/deploy/instalar-servidor.sh | bash
#
# Qué hace: instala Docker, crea memoria de intercambio (swap), descarga el repositorio en /opt/galiledu,
# genera deploy/.env con claves aleatorias y levanta la aplicación en el puerto 80.
# Guía completa: docs/guias/despliegue.md
set -euo pipefail

REPO="${REPO:-https://github.com/Yatako2304/GalilEdu.git}"
DIR="${DIR:-/opt/galiledu}"

echo "▶ 1/5 Instalando Docker…"
sudo apt-get update -qq
sudo apt-get install -y -qq docker.io docker-compose-v2 git curl openssl >/dev/null
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

echo "▶ 2/5 Memoria de intercambio (para compilar en instancias pequeñas)…"
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile >/dev/null
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

echo "▶ 3/5 Descargando el código en $DIR…"
if [ ! -d "$DIR/.git" ]; then
  sudo git clone -q "$REPO" "$DIR"
fi
sudo chown -R "$USER":"$USER" "$DIR"

echo "▶ 4/5 Configuración (deploy/.env)…"
ENV_FILE="$DIR/deploy/.env"
if [ ! -f "$ENV_FILE" ]; then
  # IP pública de la instancia (servicio de metadatos de EC2, IMDSv2). Fuera de AWS queda "localhost".
  TOKEN=$(curl -fsS -m 2 -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 60" 2>/dev/null || true)
  IP=$(curl -fsS -m 2 -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo localhost)
  cp "$DIR/deploy/.env.example" "$ENV_FILE"
  sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$(openssl rand -hex 24)|" "$ENV_FILE"
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -hex 32)|" "$ENV_FILE"
  sed -i "s|^URL_PUBLICA=.*|URL_PUBLICA=http://$IP|" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  echo "   Creado $ENV_FILE (URL_PUBLICA=http://$IP). Edítalo para agregar el correo (MAIL_*)."
fi

echo "▶ 5/5 Levantando la aplicación (la primera vez tarda varios minutos)…"
sudo bash "$DIR/deploy/actualizar.sh"

echo ""
echo "✅ Listo. Abre: $(grep '^URL_PUBLICA=' "$ENV_FILE" | cut -d= -f2)"
echo "   Cierra y vuelve a abrir la sesión SSH para usar 'docker' sin sudo."
