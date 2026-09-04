# Infraestrutura de produção do SReS

## Estado

Current: Compose de produção, rede interna, persistência, bootstrap idempotente e automação Nginx versionada descritos nesta task.

Current: scripts locais de instalação, deploy por SHA, healthcheck HTTP, rollback com lock e estado de releases, CI/GHCR e workflow de solicitação de deploy SSH.

Planned: VPS provisionada, execução remota habilitada, certificados TLS emitidos, sudoers/SSH efetivos, backups automatizados e healthcheck externo.

## Topologia

```text
Cloudflare
  └── Nginx no host (configurado por templates/scripts)
        ├── api.sres.morfeu.cloud ──► 127.0.0.1:18081 ──► api:8080
        ├── auth.sres.morfeu.cloud ─► 127.0.0.1:18083 ──► keycloak:8080
        └── s3.sres.morfeu.cloud ───► 127.0.0.1:18084 ──► minio:9000

api ── sres_internal ── postgres:5432
                    ├── minio:9000
                    └── keycloak:8080
```

O Compose cria a rede nomeada `sres_internal`. A API, Keycloak e API S3 do MinIO ficam vinculados exclusivamente ao loopback do host; PostgreSQL e console MinIO não têm publicação de porta. Cloudflare e Nginx são a entrada pública.

### Exposição pública e firewall

A VPS deve permitir publicamente somente SSH, TCP 80 e TCP 443. As portas operacionais `18081` (API), `18083` (Keycloak) e `18084` (MinIO S3) são exclusivamente loopback e não devem ser abertas no firewall. PostgreSQL e o console MinIO permanecem sem exposição pública.

## Serviços

| Serviço | Imagem | Função | Exposição |
|---|---|---|---|
| `api` | `${SRES_API_IMAGE}` | API Spring Boot e Flyway | `127.0.0.1:18081:8080` |
| `postgres` | `postgres:17.6` | Banco SReS e schema Keycloak | somente rede interna |
| `minio` | `minio/minio:RELEASE.2025-09-07T16-13-09Z` | objetos privados | somente rede interna |
| `keycloak` | `quay.io/keycloak/keycloak:26.3.3` | identidade produtiva | `127.0.0.1:18083:8080` |

As versões acompanham as imagens já usadas e validadas no desenvolvimento, evitando upgrade desnecessário nesta task. A imagem da API é externa e versionável; no futuro será publicada no GHCR pela SHA do commit.

## Persistência

Volumes nomeados:

- `sres_postgres_data`;
- `sres_minio_data`.

Keycloak usa a mesma instância PostgreSQL, com schema lógico separado `keycloak`. O arquivo `postgres-init/01-keycloak-schema.sql` cria esse schema quando o volume PostgreSQL é inicializado pela primeira vez. Esta é a opção mais simples para uma VPS única e preserva o banco da API no schema `public`; isolamento por usuário PostgreSQL separado pode ser evoluído antes de produção crítica.

Em volume já existente, scripts de init do Postgres não são executados novamente. A preparação de um ambiente existente precisa verificar `CREATE SCHEMA IF NOT EXISTS keycloak` por procedimento administrativo, sem apagar dados.

O Compose não remove volumes. Não usar `docker compose down -v`, `docker volume rm` ou `docker system prune` no fluxo normal.

## PostgreSQL

O banco recebe `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD` de `app.env`. A API usa `SPRING_DATASOURCE_URL`, username e password coerentes. O healthcheck usa `pg_isready` e a API só depende do serviço após estado saudável.

Flyway é executado pela API no startup. `ddl-auto=validate` permanece responsabilidade do backend. Rollback de imagem não reverte migrations.

## MinIO

MinIO usa bucket privado `sres-reports`, credenciais externas e volume persistente. A API S3 é publicada somente em `127.0.0.1:18084:9000` para o Nginx; o console `9001` não é publicado. O `StorageService` cria o bucket de forma idempotente quando a aplicação precisa utilizá-lo.

## Keycloak

Keycloak usa modo `start`, persistência no schema `keycloak` do PostgreSQL e healthcheck no management port interno 9000. Credenciais administrativas são somente placeholders no exemplo e devem ser fornecidas na VPS.

O Compose não monta nem importa `infra/keycloak/sres-dev-realm.json`. Realm, client, issuer, usuários e roles produtivos serão tratados em procedimento próprio. O `SRES_KEYCLOAK_ISSUER` deve ser `https://auth.sres.morfeu.cloud/realms/sres`.

## API e integrações

O container da API recebe runtime por `app.env`, usa porta interna 8080 e healthcheck em `/actuator/health`. O Dockerfile existente preserva usuário não-root.

Ollama não é serviço do Compose. `SRES_OLLAMA_ENABLED=false` é o padrão. Telegram também não é serviço e `SRES_TELEGRAM_ENABLED=false` é o padrão; token, quando ativado, ficará apenas na VPS.

## Healthchecks e dependências

- PostgreSQL: `pg_isready`.
- MinIO: `mc ready local`.
- Keycloak: conexão TCP ao management port 9000, habilitado por `KC_HEALTH_ENABLED=true`.
- API: `wget` contra `/actuator/health` e confirmação de `status=UP`.

`depends_on` ordena o primeiro startup, mas não substitui retry/reconexão da aplicação. Healthcheck externo e operação na VPS permanecem para tasks posteriores.

## Operação de releases

`sres-deploy` e `sres-rollback` serializam operações com `flock`, aceitam somente SHA completa e gravam `current`/`previous` atomicamente dentro de `/opt/sres/releases`. O healthcheck usa `/actuator/health`, HTTP 200 e `status=UP` antes de atualizar o estado. Nenhum fluxo remove volumes ou tenta reverter o schema Flyway. A instalação copia Compose, init SQL, exemplos e scripts, preservando os arquivos reais de configuração.

## Bootstrap e Nginx

`deploy/bootstrap.sh` valida Linux, Docker, Compose, Nginx, portas 18081/18083/18084 e sudoers antes de preparar o host. Cria/valida `sres-deploy`, instala scripts root-owned, preserva envs e aplica os templates `deploy/nginx/` por meio de `sres-nginx-check` e `sres-nginx-apply`. O apply limita-se aos arquivos do SReS, executa `nginx -t` antes de `systemctl reload nginx` e restaura os arquivos SReS em falha.

TLS permanece um procedimento posterior na VPS: Nginx HTTP válido, `nginx -t`, Certbot, HTTPS, novo `nginx -t` e reload. Cloudflare pode responder pelos seus próprios IPs em `dig`; não há automação da API Cloudflare.

## Workflow de deploy

O workflow `.github/workflows/deploy-production.yml` é disparado por alterações de runtime relevantes em `main` ou manualmente. Ele chama o workflow reutilizável de publicação somente após a validação e usa a SHA completa do commit como release. O job de deploy pertence ao GitHub Environment `production`, é protegido por `PRODUCTION_DEPLOY_ENABLED` e possui concurrency exclusiva sem cancelamento.

Com a flag habilitada, o workflow configura uma chave privada e `known_hosts` fornecidos por secrets, valida host/porta/usuário não-root e executa somente `sudo /usr/local/bin/sres-deploy '<SHA>'`. Nenhum segredo de runtime atravessa o SSH. A VPS deve possuir a autenticação de leitura do GHCR e a configuração `app.env`/`deploy.env` localmente. A ausência de configuração real da VPS mantém a execução remota desabilitada.

## Limitações desta etapa

- Não há execução real na VPS, emissão de certificados ou configuração de DNS; esses atos ficam para a TASK-006.
- Não há backups automatizados nem teste de restauração.
- Não há limites de CPU/RAM, pois a capacidade da VPS ainda não foi medida.
- Não há importação de realm produtivo.
- Swagger produtivo é configurável pelos envs, mas política de exposição será fechada com Nginx/TLS.
