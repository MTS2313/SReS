# Compose de produção do SReS

## Estado

Current: estrutura, Compose, instalação local, bootstrap idempotente, templates/scripts Nginx, deploy por SHA, healthcheck, rollback sem alteração de volumes/migrations, CI do backend, publicação da imagem no GHCR e workflow de deploy SSH protegido.

Planned: configuração real da VPS, Nginx/TLS e execução remota com secrets configurados.

## Arquivos

- `compose.prod.yml` — serviços de produção para uma VPS.
- `app.env.example` — configuração de runtime e placeholders de secrets.
- `deploy.env.example` — imagem, portas e parâmetros do Compose.
- `INFRASTRUCTURE.md` — topologia, persistência e limitações.
- `postgres-init/01-keycloak-schema.sql` — cria o schema lógico do Keycloak em um banco PostgreSQL novo.
- `scripts/install.sh` — instala a estrutura sem sobrescrever configurações existentes.
- `scripts/sres-deploy` — faz deploy de uma SHA completa com lock e rollback automático.
- `scripts/sres-healthcheck` — valida HTTP 200 e JSON com `status=UP`.
- `scripts/sres-rollback` — alterna para a release anterior conhecida.
- `scripts/sres-common` — funções compartilhadas e leitura segura de `deploy.env`.
- `tests/validate-compose.sh` — validação estrutural do Compose.
- `tests/test-scripts.sh` — testes locais com fakes, sem tocar em Docker ou VPS.
- `tests/test-deploy-workflow.sh` — validação estrutural do workflow de deploy SSH.
- `tests/test-nginx-bootstrap.sh` — validação estrutural do bootstrap, Compose e Nginx.

`app.env` contém configuração da aplicação e dependências. `deploy.env` contém somente parâmetros do mecanismo de composição/deploy. Os arquivos reais devem existir fora do Git, em `/opt/sres/app.env` e `/etc/sres/deploy.env`, com modo 600.

Os domínios operacionais são `api.sres.morfeu.cloud`, `auth.sres.morfeu.cloud` e `s3.sres.morfeu.cloud`. Os binds de host são exclusivamente loopback: API `18081`, Keycloak `18083` e MinIO S3 `18084`. PostgreSQL e o console MinIO não possuem porta publicada.

A exposição pública permitida na VPS é limitada a SSH, TCP 80 e TCP 443. As portas operacionais `18081`, `18083` e `18084` permanecem exclusivamente em loopback; PostgreSQL e o console MinIO continuam sem exposição pública.

## Instalação e operação

Em uma VPS com Docker, Compose plugin e Nginx instalados, `bootstrap.sh` valida o sistema e as portas, cria/valida `sres-deploy`, chama `scripts/install.sh`, instala sudoers limitado e aplica somente a configuração Nginx do SReS após `nginx -t`. Não gera secrets, não sobrescreve `app.env`/`deploy.env`, não remove recursos alheios e não executa deploy de imagem.

O bootstrap exige root. Em caso de porta ocupada, pré-requisito ausente ou sudoers inválido, aborta antes da preparação correspondente. Certbot ausente é reportado; a emissão de certificados fica para a preparação TLS na VPS, depois de DNS e Nginx HTTP válidos.

O `sres-deploy` recebe somente uma SHA Git completa de 40 caracteres:

```bash
sres-deploy 0123456789abcdef0123456789abcdef01234567
sres-healthcheck
sres-rollback
```

O repositório da imagem é lido de `SRES_API_IMAGE` em `deploy.env`; a tag é formada internamente como `<repository>:<sha>`. O estado fica em `/opt/sres/releases/current` e `previous`, com escrita atômica e lock em `/var/lock/sres-deploy.lock`. A segunda execução concorrente falha.

O healthcheck exige `curl`, usa URL, retries e timeout configuráveis e não imprime o corpo da resposta. O login no GHCR privado é pré-requisito operacional da VPS e não é feito por argumento nem registrado pelos scripts.

Falha de pull, startup ou health tenta restaurar a release anterior conhecida. Sem release anterior, o erro é explícito. Rollback de imagem nunca desfaz migration Flyway, executa `clean` ou remove volumes.

## Validação sem secrets

Crie arquivos temporários fora do repositório, com valores descartáveis e sem credenciais reais:

```bash
tmp_app="$(mktemp)"
tmp_deploy="$(mktemp)"
trap 'rm -f "$tmp_app" "$tmp_deploy"' EXIT
cp deploy/app.env.example "$tmp_app"
sed -i 's#keycloak.example.invalid#keycloak.example.test#g; s/CHANGE_ME/test-only/g' "$tmp_app"
cat > "$tmp_deploy" <<EOF
SRES_API_IMAGE=sres-api:test
SRES_API_HOST_PORT=28080
SRES_KEYCLOAK_HOST_PORT=28081
SRES_COMPOSE_PROJECT=sres-compose-test
SRES_APP_ENV_FILE=$tmp_app
EOF
docker compose --project-name sres-compose-test --env-file "$tmp_deploy" -f deploy/compose.prod.yml config
deploy/tests/validate-compose.sh "$tmp_deploy"
```

Não versione os temporários. `docker compose config` somente renderiza a configuração; o smoke executável deve usar volumes explicitamente descartáveis e identificados como teste.

## Smoke local descartável

O smoke completo requer uma imagem `sres-api:test` construída e valores de teste coerentes. Execute-o somente com projeto, portas e volumes `sres-compose-test` criados para esse fim. Depois remova apenas esses recursos identificados, nunca os volumes `sres-dev-*` ou qualquer alvo implícito.

```bash
docker build -f backend/Dockerfile -t sres-api:test .
docker compose --project-name sres-compose-test --env-file "$tmp_deploy" -f deploy/compose.prod.yml up -d --wait
docker compose --project-name sres-compose-test --env-file "$tmp_deploy" -f deploy/compose.prod.yml ps
docker compose --project-name sres-compose-test --env-file "$tmp_deploy" -f deploy/compose.prod.yml down
```

O smoke deve usar `set -euo pipefail` e só pode remover os volumes `sres-compose-test-*` criados explicitamente para ele. Em banco já inicializado, o init SQL do PostgreSQL não é reexecutado; a criação do schema `keycloak` deve ser verificada/aplicada por procedimento administrativo antes de iniciar o stack.

A imagem precisa acessar o banco e MinIO pelos nomes internos. Keycloak usa o schema `keycloak`; não há importação de realm de desenvolvimento. O bucket `sres-reports` é criado pela aplicação de forma idempotente quando necessário.

## Nginx e TLS

Os templates versionados em `nginx/` definem somente os sites SReS. `sres-nginx-check` renderiza e valida sem modificar `/etc/nginx`; `sres-nginx-apply` preserva backups dos sites SReS, não toca em sites alheios, executa `nginx -t` antes de reload e restaura a configuração SReS em caso de falha. O MinIO expõe apenas a API S3 por `s3.sres.morfeu.cloud`; o console não recebe rota.

O TLS deve seguir, na VPS, `Nginx HTTP → nginx -t → Certbot/Let's Encrypt → HTTPS → nginx -t → reload`. Não emitir certificados em testes locais. Cloudflare pode retornar IPs próprios em consultas DNS, o que não invalida a configuração.

## Interface privilegiada futura

O usuário dedicado `sres-deploy` deverá receber, em configuração de VPS posterior, sudo restrito somente à interface validada `/usr/local/bin/sres-deploy`; rollback manual deverá seguir política administrativa própria. Esta task não configura sudoers, usuário ou acesso SSH.

## CI, GHCR e deploy por workflow

`/.github/workflows/backend.yml` executa `scripts/validate.sh` em pull requests e pushes relevantes, com Java 21 Temurin e cache Maven. `/.github/workflows/publish-image.yml` é reutilizável pelo deploy e também pode ser executado por `workflow_dispatch`; a publicação automática de `main` é coordenada por `deploy-production.yml`, evitando dois workflows independentes publicarem a mesma SHA.

A imagem usa o repositório `ghcr.io/<owner-lowercase>/sres-api` e a SHA completa de `github.sha` como tag imutável. O workflow usa somente `GITHUB_TOKEN` com `packages: write` no job de publicação. O pacote permanece privado e a autenticação de leitura da VPS será configurada fora destes workflows, em task posterior.

O build usa o contexto raiz do monorepo e `./backend/Dockerfile`. O workflow `deploy-production.yml` valida/publica antes do job remoto, usa o Environment `production` e a variável `PRODUCTION_DEPLOY_ENABLED`. Quando a variável não é `true`, o job SSH é pulado; nenhum runtime secret é enviado ao GitHub Actions.

Quando habilitado, o job exige os secrets `VPS_HOST`, `VPS_PORT`, `VPS_USER`, `VPS_SSH_PRIVATE_KEY` e `VPS_SSH_KNOWN_HOSTS`. Usa conta dedicada não-root, `BatchMode`, `StrictHostKeyChecking=yes` e chama exclusivamente `sudo /usr/local/bin/sres-deploy '<SHA completa>'`. A VPS deve estar previamente autenticada no GHCR. O workflow usa concurrency `sres-production` sem cancelamento de deploy em andamento. O Environment pode receber aprovação manual no futuro.

O workflow não executa Compose, `docker login`, `git pull` ou bootstrap remoto. A preparação do usuário, sudoers, GHCR e arquivos de configuração continua sendo requisito da task de bootstrap da VPS.

## Próximas etapas

Esta implementação não conecta na VPS nem configura secrets reais. A TASK-006 executará o bootstrap real, preparará credenciais/DNS/TLS e comprovará o deploy ponta a ponta.

Nunca coloque senhas, tokens, chaves SSH ou exports de realm produtivo neste diretório.
