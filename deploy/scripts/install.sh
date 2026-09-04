#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="${SRES_SOURCE_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
INSTALL_ROOT="${SRES_INSTALL_ROOT:-/opt/sres}"
CONFIG_DIR="${SRES_INSTALL_CONFIG_DIR:-/etc/sres}"
BIN_DIR="${SRES_INSTALL_BIN_DIR:-/usr/local/bin}"
INSTALL_OWNER="${SRES_INSTALL_OWNER:-}"

[[ -d "$SOURCE_ROOT/deploy" ]] || { echo "deploy source ausente: $SOURCE_ROOT/deploy" >&2; exit 1; }
mkdir -p "$INSTALL_ROOT/releases" "$INSTALL_ROOT/postgres-init" "$CONFIG_DIR" "$BIN_DIR"

install -m 644 "$SOURCE_ROOT/deploy/compose.prod.yml" "$INSTALL_ROOT/compose.prod.yml"
install -m 644 "$SOURCE_ROOT/deploy/postgres-init/01-keycloak-schema.sql" "$INSTALL_ROOT/postgres-init/01-keycloak-schema.sql"
install -m 644 "$SOURCE_ROOT/deploy/app.env.example" "$INSTALL_ROOT/app.env.example"
install -m 644 "$SOURCE_ROOT/deploy/deploy.env.example" "$CONFIG_DIR/deploy.env.example"

for script in sres-common sres-healthcheck sres-deploy sres-rollback; do
  install -m 755 "$SOURCE_ROOT/deploy/scripts/$script" "$BIN_DIR/$script"
done

if [[ ! -e "$INSTALL_ROOT/app.env" ]]; then
  touch "$INSTALL_ROOT/app.env"
  chmod 600 "$INSTALL_ROOT/app.env"
fi
if [[ ! -e "$CONFIG_DIR/deploy.env" ]]; then
  touch "$CONFIG_DIR/deploy.env"
  chmod 600 "$CONFIG_DIR/deploy.env"
fi

if [[ -n "$INSTALL_OWNER" ]]; then
  [[ "$INSTALL_OWNER" =~ ^[a-z_][a-z0-9_-]*:[a-z_][a-z0-9_-]*$ ]] || { echo "SRES_INSTALL_OWNER inválido" >&2; exit 1; }
  chown -R "$INSTALL_OWNER" "$INSTALL_ROOT" "$CONFIG_DIR"
  chown "$INSTALL_OWNER" "$BIN_DIR"/sres-common "$BIN_DIR"/sres-healthcheck "$BIN_DIR"/sres-deploy "$BIN_DIR"/sres-rollback
fi

chmod 700 "$INSTALL_ROOT" "$CONFIG_DIR"
chmod 600 "$INSTALL_ROOT/app.env" "$CONFIG_DIR/deploy.env"
echo "Estrutura SReS instalada sem sobrescrever configurações existentes."
