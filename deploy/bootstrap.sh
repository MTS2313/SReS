#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="${SRES_SOURCE_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
INSTALL_ROOT="${SRES_INSTALL_ROOT:-/opt/sres}"
CONFIG_DIR="${SRES_INSTALL_CONFIG_DIR:-/etc/sres}"
BIN_DIR="${SRES_INSTALL_BIN_DIR:-/usr/local/bin}"
SUDOERS_DIR="${SRES_SUDOERS_DIR:-/etc/sudoers.d}"
NGINX_CONFIG_DIR="${SRES_NGINX_CONFIG_DIR:-/etc/nginx}"

die() { printf '%s\n' "ERRO: $*" >&2; exit 1; }
require_command() { command -v "$1" >/dev/null 2>&1 || die "comando obrigatório ausente: $1"; }

[[ "$EUID" -eq 0 ]] || die "bootstrap requer root; execute com sudo"
[[ "$(uname -s)" == Linux ]] || die "sistema não suportado: apenas Linux"
[[ -d "$SOURCE_ROOT/deploy" ]] || die "diretório deploy ausente: $SOURCE_ROOT/deploy"
require_command docker
require_command nginx
require_command ss
docker compose version >/dev/null 2>&1 || die "Docker Compose plugin ausente"
if ! command -v certbot >/dev/null 2>&1; then
  printf '%s\n' "Aviso: certbot não está instalado; TLS deverá ser preparado antes da emissão." >&2
fi

check_port_free() {
  local port="$1"
  if ss -H -ltn "sport = :$port" 2>/dev/null | grep -q .; then
    die "porta TCP $port já está ocupada; nenhuma porta alternativa será escolhida"
  fi
}
check_port_free 18081
check_port_free 18083
check_port_free 18084

if getent passwd sres-deploy >/dev/null 2>&1; then
  deploy_uid="$(id -u sres-deploy)"
  [[ "$deploy_uid" -ne 0 ]] || die "sres-deploy não pode ser root"
  deploy_shell="$(getent passwd sres-deploy | cut -d: -f7)"
  [[ "$deploy_shell" != */nologin && "$deploy_shell" != */false ]] || die "sres-deploy precisa de shell para executar comandos SSH restritos"
  id -nG sres-deploy | tr ' ' '\n' | grep -qx docker && die "sres-deploy não pode pertencer ao grupo docker"
else
  require_command useradd
  useradd --system --create-home --home-dir /var/lib/sres-deploy --shell /bin/bash sres-deploy
fi
deploy_home="$(getent passwd sres-deploy | cut -d: -f6)"
[[ -n "$deploy_home" && -d "$deploy_home" ]] || die "home de sres-deploy ausente"
chown sres-deploy:sres-deploy "$deploy_home"
chmod 750 "$deploy_home"

SRES_SOURCE_ROOT="$SOURCE_ROOT" \
SRES_INSTALL_ROOT="$INSTALL_ROOT" \
SRES_INSTALL_CONFIG_DIR="$CONFIG_DIR" \
SRES_INSTALL_BIN_DIR="$BIN_DIR" \
  "$SCRIPT_DIR/scripts/install.sh"

mkdir -p "$SUDOERS_DIR"
sudoers_file="$SUDOERS_DIR/sres-deploy"
sudoers_tmp="$(mktemp "$SUDOERS_DIR/.sres-deploy.XXXXXX")"
trap 'rm -f -- "$sudoers_tmp"' EXIT
awk '{gsub(/^%sres-deploy/, "sres-deploy"); print}' "$SOURCE_ROOT/deploy/sudoers/sres-deploy" > "$sudoers_tmp"
chown root:root "$sudoers_tmp"
chmod 440 "$sudoers_tmp"
if command -v visudo >/dev/null 2>&1; then
  visudo -cf "$sudoers_tmp" >/dev/null || die "sudoers inválido; configuração existente preservada"
else
  die "visudo ausente; não é seguro instalar sudoers sem validação"
fi
mv -f -- "$sudoers_tmp" "$sudoers_file"
trap - EXIT

SRES_ROOT_DIR="$INSTALL_ROOT" \
SRES_CONFIG_DIR="$CONFIG_DIR" \
SRES_DEPLOY_ENV_FILE="$CONFIG_DIR/deploy.env" \
SRES_NGINX_TEMPLATE_DIR="$INSTALL_ROOT/nginx" \
SRES_NGINX_CONFIG_DIR="$NGINX_CONFIG_DIR" \
  "$BIN_DIR/sres-nginx-check"
SRES_ROOT_DIR="$INSTALL_ROOT" \
SRES_CONFIG_DIR="$CONFIG_DIR" \
SRES_DEPLOY_ENV_FILE="$CONFIG_DIR/deploy.env" \
SRES_NGINX_TEMPLATE_DIR="$INSTALL_ROOT/nginx" \
SRES_NGINX_CONFIG_DIR="$NGINX_CONFIG_DIR" \
  "$BIN_DIR/sres-nginx-apply"

printf '%s\n' "Bootstrap SReS concluído sem deploy produtivo."
