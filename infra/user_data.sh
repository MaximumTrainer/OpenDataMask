#!/bin/bash
# EC2 bootstrap script (Amazon Linux 2023)
# Runs as root on first boot via cloud-init user_data.
# Installs Docker + Compose, sets up /opt/opendatamask/, and creates a
# systemd service so docker-compose starts automatically on reboot.

set -euo pipefail

# ── System update ────────────────────────────────────────────────────────────────
dnf update -y

# ── Install Docker ───────────────────────────────────────────────────────────────
dnf install -y docker
systemctl enable docker
systemctl start docker

# Add default user to docker group so SSH deploys work without sudo
usermod -aG docker ec2-user

# ── Install Docker Compose plugin ────────────────────────────────────────────────
mkdir -p /usr/local/lib/docker/cli-plugins
COMPOSE_VERSION="v2.24.5"
curl -SL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Also expose as a standalone binary for scripts
ln -sf /usr/local/lib/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose

# ── Install utilities ────────────────────────────────────────────────────────────
dnf install -y wget curl git

# ── Create application directory ─────────────────────────────────────────────────
APP_DIR="/opt/opendatamask"
mkdir -p "${APP_DIR}"
chown ec2-user:ec2-user "${APP_DIR}"

# ── Create systemd service for auto-start ────────────────────────────────────────
cat > /etc/systemd/system/opendatamask.service << 'EOF'
[Unit]
Description=OpenDataMask Application Stack
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/opendatamask
EnvironmentFile=/opt/opendatamask/.env
ExecStart=/usr/local/bin/docker-compose up -d --remove-orphans
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=300
User=ec2-user
Group=ec2-user

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable opendatamask.service

echo "Bootstrap complete. OpenDataMask will start automatically after docker-compose.yml and .env are deployed."
