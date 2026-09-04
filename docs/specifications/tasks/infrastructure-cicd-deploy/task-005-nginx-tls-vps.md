# TASK-005 — Bootstrap, Nginx/TLS e preparação reproduzível da VPS

## Estado

approved

## Objetivo

Implementar e validar toda a automação versionada necessária para que um administrador prepare a VPS do SReS executando um único `deploy/bootstrap.sh`. A task não exige que o agente tenha acesso SSH à VPS e não executa o deploy final; a execução real e a validação ponta a ponta pertencem à TASK-006.

## Decisões fechadas

- Usar o Docker e o Compose plugin já instalados na VPS; não instalar Kubernetes, k3s ou runtime alternativo.
- Nginx roda no host e é administrado somente por templates e scripts versionados.
- API: `api.sres.morfeu.cloud`.
- Keycloak: `auth.sres.morfeu.cloud`.
- MinIO S3: `s3.sres.morfeu.cloud`.
- API: `127.0.0.1:18081` → container `8080`.
- Keycloak: `127.0.0.1:18083` → container `8080`.
- MinIO S3: `127.0.0.1:18084` → container `9000`.
- PostgreSQL não possui porta publicada no host.
- Console MinIO não possui porta publicada nem domínio público.
- Ollama permanece fora do Compose e desabilitado inicialmente.
- Telegram permanece desabilitado inicialmente.
- Nginx usa Certbot/Let's Encrypt sem remover ou substituir configurações de outros serviços.

## Escopo

### Bootstrap versionado

Implementar ou ajustar `deploy/bootstrap.sh` para:

- exigir root/sudo quando necessário;
- verificar Linux suportado, Docker, Compose plugin, Nginx e Certbot ou informar claramente o que falta;
- verificar se as portas 18081, 18083 e 18084 estão livres antes de alterar qualquer coisa;
- abortar com diagnóstico se uma porta estiver ocupada, sem escolher outra silenciosamente;
- criar `/opt/sres`, `/opt/sres/releases` e `/etc/sres`;
- criar ou validar o usuário não-root `sres-deploy`;
- instalar scripts root-owned em `/usr/local/bin`;
- instalar sudoers restrito, validado por `visudo -cf`;
- preparar Compose e arquivos de exemplo sem substituir `app.env` ou `deploy.env` existentes;
- preparar a configuração Nginx usando os scripts versionados;
- ser idempotente;
- nunca remover volumes, containers, networks ou arquivos de outros projetos;
- nunca executar deploy produtivo automaticamente.

O bootstrap não deve gerar secrets, copiar chaves privadas, configurar DNS, emitir certificados sem pré-condições ou sobrescrever arquivos reais de configuração.

### Contrato da VPS

Após o bootstrap, a estrutura deve ser equivalente a:

```text
/opt/sres/
├── compose.prod.yml
├── app.env
├── releases/
└── current-release / estado equivalente

/etc/sres/
└── deploy.env

/usr/local/bin/
├── sres-deploy
├── sres-healthcheck
├── sres-rollback
├── sres-nginx-check
└── sres-nginx-apply
```

`sres-deploy` deve permanecer o único comando concedido ao usuário do GitHub Actions via sudo. Os scripts executados via sudo devem ser root-owned e não graváveis por `sres-deploy`; o usuário não deve entrar no grupo Docker.

### Templates e scripts Nginx

Criar estrutura equivalente a:

```text
deploy/nginx/
├── sres-api.conf.template
├── sres-auth.conf.template
└── sres-s3.conf.template
```

Criar:

```text
deploy/scripts/sres-nginx-check
deploy/scripts/sres-nginx-apply
```

O fluxo operacional deve ser:

```text
templates versionados
  → sres-nginx-check
  → sres-nginx-apply
  → nginx -t
  → systemctl reload nginx
```

`sres-nginx-check` valida sem reload:

- os três domínios definidos;
- upstreams `127.0.0.1:18081`, `127.0.0.1:18083` e `127.0.0.1:18084`;
- headers encaminhados;
- `client_max_body_size 16m` ou margem equivalente;
- ausência de upstream público indevido;
- renderização e `nginx -t` no host apropriado.

`sres-nginx-apply` deve gerar somente arquivos do SReS, preservar sites alheios, manter backup da configuração anterior do SReS, executar `nginx -t` antes de qualquer reload e restaurar/abortar quando a configuração for inválida. Deve usar reload, não restart, quando suficiente, e ser idempotente.

### Server blocks

Configurar:

- `api.sres.morfeu.cloud` → `127.0.0.1:18081`;
- `auth.sres.morfeu.cloud` → `127.0.0.1:18083`;
- `s3.sres.morfeu.cloud` → `127.0.0.1:18084`, somente API S3.

Os proxies devem enviar `Host`, `X-Real-IP`, `X-Forwarded-For` e `X-Forwarded-Proto`, usar HTTP/1.1 quando necessário e manter limites compatíveis com PDF de 10 MB. Não criar rota para o console MinIO.

### Keycloak e TLS

Preparar configuração compatível com Keycloak 26 e proxy moderno, com issuer produtivo:

```text
https://auth.sres.morfeu.cloud/realms/sres
```

Não importar automaticamente `sres-dev`, seus usuários ou credenciais.

Documentar e automatizar de forma segura, quando as pré-condições existirem, o fluxo:

```text
Nginx HTTP válido
  → nginx -t
  → Certbot/Let's Encrypt
  → HTTPS e redirect HTTP→HTTPS
  → nginx -t
  → reload seguro
```

Não emitir certificados em testes locais, não revogar certificados de outros domínios e não apagar configurações existentes. Quando o DNS usar Cloudflare/proxy, a validação deve aceitar que `dig` retorne IPs da Cloudflare; não deve exigir que o resultado seja diretamente o IP da VPS.

### Compose e configuração

Atualizar `deploy/compose.prod.yml` somente se necessário para refletir os binds de loopback definidos. Garantir:

- `127.0.0.1:18081:8080` para a API;
- `127.0.0.1:18083:8080` para Keycloak;
- `127.0.0.1:18084:9000` para MinIO S3;
- ausência de `ports` para PostgreSQL;
- ausência de porta pública para o console MinIO;
- nenhum bind `0.0.0.0` para esses serviços;
- Ollama fora do Compose;
- `SRES_OLLAMA_ENABLED=false` e `SRES_TELEGRAM_ENABLED=false` inicialmente.

Centralizar em `deploy.env.example`, sem secrets:

```text
SRES_API_DOMAIN=api.sres.morfeu.cloud
SRES_AUTH_DOMAIN=auth.sres.morfeu.cloud
SRES_S3_DOMAIN=s3.sres.morfeu.cloud
SRES_API_HOST_PORT=18081
SRES_KEYCLOAK_HOST_PORT=18083
SRES_MINIO_HOST_PORT=18084
```

Secrets reais continuam exclusivamente na VPS: banco, MinIO, Keycloak, GHCR, Telegram, certificados e chaves SSH. Examples podem ser criados apenas quando ausentes e nunca substituem arquivos existentes.

### Firewall e backups

Documentar e validar a exposição desejada: SSH, 80 e 443. As portas 18081, 18083 e 18084 são loopback; PostgreSQL e console MinIO não são públicos.

Manter como requisito operacional, sem transformar esta task em uma plataforma de backup:

- `pg_dump` periódico para PostgreSQL;
- cópia/mirror dos objetos MinIO;
- inclusão dos dados do Keycloak no PostgreSQL;
- destino externo, retenção e teste de restauração definidos antes de produção.

Se o destino externo ainda não existir, registrar a dívida sem impedir a criação do bootstrap.

## TDD e validação

Antes da implementação, criar harness falho para pelo menos:

- bootstrap ausente/incompleto;
- Docker/Compose ausente;
- porta ocupada;
- env existente não sobrescrito;
- usuário já existente;
- execução repetida idempotente;
- scripts root-owned e sudoers limitado;
- domínios e portas corretos;
- PostgreSQL e console MinIO sem publicação;
- ausência de Ollama;
- headers e `client_max_body_size`;
- `nginx -t` inválido sem reload;
- preservação de sites alheios;
- ausência de operações destrutivas e secrets versionados.

Executar Red antes da implementação, Green após o mínimo necessário e Refactor mantendo os testes verdes. Usar fake/host explicitamente identificado para Nginx e ambiente descartável para Compose; não emitir certificados nem alterar a VPS durante testes locais.

Validações esperadas:

- `scripts/test.sh`;
- `scripts/validate.sh`;
- shellcheck quando disponível;
- `nginx -t` em host de teste apropriado;
- validação de templates, portas, headers e reversão sem reload;
- `docker compose config` sem imprimir secrets.

## Fora do escopo

- deploy final e smoke externo da VPS;
- pipeline real do GitHub Actions;
- validação E2E e rollback real;
- criação de secrets reais, usuário remoto ou configuração de GitHub Environment;
- alteração de DNS;
- Kubernetes/k3s, CDN, WAF, HA e múltiplas VPS;
- frontend, CI/CD adicional, observabilidade externa e backup complexo.

## Limite com a TASK-006

### TASK-005 produz

- bootstrap reproduzível;
- scripts e templates Nginx versionados;
- Compose e portas alinhados;
- harnesses e documentação;
- procedimento seguro para configuração posterior.

### TASK-006 executa

- bootstrap na VPS real;
- secrets e credenciais reais fora do Git;
- login de leitura no GHCR;
- configuração do GitHub Environment;
- pipeline e deploy reais;
- HTTPS real e smoke externo;
- persistência, rollback e evidências ponta a ponta.

## Critérios de conclusão

- `deploy/bootstrap.sh` é idempotente, detecta pré-requisitos/conflitos e não executa deploy;
- estrutura `/opt/sres`, `/etc/sres`, usuário e permissões são reproduzíveis;
- sudoers permite somente o script aprovado e passa em `visudo -cf`;
- templates e scripts Nginx gerenciam apenas a configuração do SReS;
- API, Keycloak e MinIO usam exatamente os domínios e portas definidos;
- PostgreSQL e console MinIO não são publicados;
- `nginx -t` precede qualquer reload e falhas não causam indisponibilidade;
- TLS/Certbot possui procedimento seguro sem afetar outros sites;
- Ollama e Telegram permanecem opcionais/desabilitados;
- nenhum secret real é versionado;
- harness, `scripts/test.sh` e `scripts/validate.sh` estão verdes;
- não há operação destrutiva nem antecipação da TASK-006.

## Resposta esperada

Relatar a automação criada, pré-requisitos, domínios e portas, topologia Nginx, segurança de permissões/secrets, evidência TDD, validações locais, limitações de acesso à VPS e estado recomendado.
