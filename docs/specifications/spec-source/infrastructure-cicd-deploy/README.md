# Specification source — Infraestrutura, CI/CD e deploy do SReS

## Estado

planned — specification proposta, ainda não implementada.

## Problema

O SReS possui API MVP concluída, imagem Docker validada e ambiente local, mas não possui uma definição versionada para publicar imagens, executar CI, instalar a aplicação em uma VPS Linux, operar serviços persistentes, validar releases ou fazer rollback seguro.

## Objetivo

Definir uma infraestrutura simples, reproduzível e segura para uma única VPS, com CI/CD no GitHub Actions, imagens no GHCR, Compose de produção, Nginx/TLS, scripts operacionais, backups e rollback da aplicação sem tentar desfazer migrations.

Esta specification não implementa os arquivos descritos. A implementação ocorrerá exclusivamente pelas tasks dependentes.

## Contexto comprovado

- Projeto Maven Java 21 em `backend/`, coordenadas `br.com.sres:sres-api`.
- Spring Boot, Spring Modulith, PostgreSQL, Flyway, JPA, MinIO e Keycloak.
- Ollama e Telegram configuráveis e desabilitados por padrão.
- `backend/Dockerfile` multi-stage já validado; o contexto correto de build é a raiz do monorepo:
  `docker build -f backend/Dockerfile -t sres-api:local .`.
- `compose.yaml` atual é local e mantém PostgreSQL, MinIO e Keycloak.
- `scripts/` na raiz é a interface operacional existente.
- Não existem workflows ou `deploy/` produtivo no SReS.
- A referência solicitada `MTS2313/petstop-project` não está disponível neste workspace. Foi inspecionado `../pet-project` como referência local estrutural, sem assumir identidade de produto ou copiar seus serviços.

## Decisões da iniciativa

| ID | Decisão | Consequência |
|---|---|---|
| INFRA-001 | Uma VPS Linux inicialmente | Não haverá HA, múltiplas regiões ou múltiplas VPS nesta iniciativa |
| INFRA-002 | Nginx no host como entrada pública | Containers publicam somente localhost quando houver necessidade de integração com Nginx |
| INFRA-003 | Um Compose de produção para API, PostgreSQL, MinIO e Keycloak | Separação lógica por rede, volumes e healthchecks; sem Compose local dependente de produção |
| INFRA-004 | Imagem GHCR versionada pela SHA completa do commit | Rollback aponta para imagem imutável, nunca depende de `latest` |
| INFRA-005 | GitHub Actions valida antes de publicar/deployar | Uma imagem só é publicada depois de `scripts/test.sh` e `scripts/validate.sh` verdes |
| INFRA-006 | Secrets de runtime ficam somente na VPS | Actions recebe apenas acesso SSH, known hosts e autorização necessária ao GHCR |
| INFRA-007 | Usuário SSH dedicado sem acesso ao Docker | Sudo limitado ao script validado `/usr/local/bin/sres-deploy` |
| INFRA-008 | Ollama externo e opcional | `SRES_OLLAMA_ENABLED=false` continua válido; `SRES_OLLAMA_BASE_URL` aponta para outro host quando ativado |
| INFRA-009 | Telegram externo e opcional | Token fica na VPS; sem token a aplicação inicia normalmente |
| INFRA-010 | Flyway migra no startup da API | Health só é considerado válido após a aplicação conectar e validar/migrar o banco |
| INFRA-011 | Rollback de imagem não reverte banco | Migrations devem ser compatíveis com a versão anterior; migrations destrutivas exigem procedimento separado |
| INFRA-012 | GHCR privado por padrão | VPS usa credencial de leitura armazenada fora do repositório; publicação pública só deve ser decisão explícita |
| INFRA-013 | Swagger configurável por ambiente | Produção pode restringir/desabilitar UI e docs sem alterar segurança da API |
| INFRA-014 | Backup independente do deploy | PostgreSQL, MinIO e configuração Keycloak exigem rotina e teste de restauração |

## Arquitetura alvo

```text
Internet
  │ 80/443
  ▼
Nginx host ───────────────► sres-api:8080
  │                         │
  │                         ├── PostgreSQL:5432 (rede interna)
  │                         ├── MinIO:9000 (rede interna)
  │                         └── Keycloak:8080 (rede interna)
  │
  └── keycloak.<domínio> ─► Keycloak, somente se autenticação externa exigir endpoint público

Ollama externo opcional ◄── SRES_OLLAMA_BASE_URL
Telegram externo opcional ─ token somente em app.env
```

Domínios são parâmetros, não valores decididos: pelo menos um domínio da API e um domínio do Keycloak serão necessários. O domínio do MinIO não é necessário para o contrato atual de downloads pela API e não deve ser público por padrão.

## Estrutura proposta

```text
deploy/
├── compose.prod.yml
├── app.env.example
├── deploy.env.example
├── README.md
├── INFRASTRUCTURE.md
├── bootstrap.sh
├── nginx/
│   ├── sres-nginx-check
│   └── sres-nginx-apply
└── scripts/
    ├── install.sh
    ├── sres-deploy
    ├── sres-healthcheck
    └── sres-rollback
.github/workflows/
├── backend.yml
└── deploy-production.yml
```

Os nomes são o desenho inicial; a task de implementação poderá ajustar nomes sem perder os contratos de operação.

## Imagens e GHCR

A imagem da API será publicada como:

```text
ghcr.io/<owner>/sres-api:<git-sha-completa>
```

Regras:

- SHA deve ter 40 caracteres hexadecimais e ser validada no host.
- `latest` não é referência de deploy nem de rollback.
- Uma tag auxiliar como `main` pode existir apenas como conveniência, nunca como release operacional.
- O build deve usar BuildKit/cache e `GITHUB_TOKEN` com `packages: write`.
- GHCR privado é a recomendação inicial; a VPS terá token de leitura dedicado, fora do Git e com modo restrito.
- O token de pull não deve aparecer em logs, argumentos persistidos, imagem ou Compose versionado.

## CI backend

`backend.yml` deve executar em pull requests e pushes que alterem `backend/**`, `scripts/**` relevantes, `compose.yaml`, `infra/**`, `backend/Dockerfile`, `.dockerignore` ou o próprio workflow. Alteração exclusivamente documental não deve publicar imagem.

O job deve usar checkout, Java 21 Temurin, cache Maven e os scripts oficiais da raiz:

1. `scripts/test.sh` — suíte completa, incluindo Testcontainers;
2. `scripts/validate.sh` — validação global, sem repetir a suíte se o script já a executar.

O desenho final deve evitar rodar a mesma suíte pesada duas vezes. Se `validate.sh` chamar os testes, o CI executará somente o fluxo oficial uma vez e fará checks adicionais separados quando necessário.

## Pipeline de produção

`deploy-production.yml`:

```text
push na main (paths relevantes) ou workflow_dispatch
  → testes/validação
  → build multi-stage
  → push GHCR:<SHA>
  → GitHub Environment production
  → se PRODUCTION_DEPLOY_ENABLED=true, SSH restrito
  → sudo /usr/local/bin/sres-deploy <SHA>
  → health check + smoke
```

Configuração obrigatória:

- `environment: production`;
- `concurrency.group: sres-production`;
- `cancel-in-progress: false`;
- `permissions: contents: read, packages: write`;
- `workflow_dispatch`;
- job de deploy condicionado a `vars.PRODUCTION_DEPLOY_ENABLED == 'true'`.

Quando desabilitado, o pipeline testa e publica a imagem, mas não acessa a VPS.

## GitHub Secrets, Variables e Environment

Secrets mínimos no Environment `production`:

```text
VPS_HOST
VPS_PORT
VPS_USER                 # sres-deploy
VPS_SSH_PRIVATE_KEY
VPS_SSH_KNOWN_HOSTS
```

Variables não sensíveis:

```text
PRODUCTION_DEPLOY_ENABLED
```

Não colocar no GitHub Actions secrets de PostgreSQL, MinIO, Keycloak, Telegram ou Ollama quando eles podem permanecer exclusivamente na VPS. Não criar `VPS_SSH_KNOWN_HOSTS` dinamicamente nem usar `StrictHostKeyChecking=no`.

O Environment pode receber required reviewers posteriormente; isso não é obrigatório para a primeira implementação, mas deve ser documentado.

## SSH e sudo

Recomendação:

- usuário `sres-deploy` sem shell operacional amplo, sem grupo `docker` e sem root direto;
- chave exclusiva do GitHub Actions em `authorized_keys`;
- sudoers permitindo somente `/usr/local/bin/sres-deploy` com argumento validado;
- rollback manual deve ser uma operação administrativa separada e não automaticamente concedida ao usuário do Actions;
- nenhum comando arbitrário concatenado pela workflow.

O script deve validar SHA antes de qualquer uso em Compose. O wildcard do sudoers, se inevitável para passar o SHA, é mitigado pela validação estrita dentro do script e pela ausência de permissão Docker/shell ao usuário.

## Estrutura na VPS

```text
/opt/sres/
├── compose.prod.yml
├── app.env                 # modo 600, secrets/configuração de runtime
├── releases/
│   └── history.log
└── current-release
/etc/sres/deploy.env        # modo 600, parâmetros de deploy
/usr/local/bin/sres-deploy
/usr/local/bin/sres-healthcheck
/usr/local/bin/sres-rollback
/var/lock/sres-deploy.lock
```

O instalador deve criar diretórios sem sobrescrever `app.env` ou `deploy.env` existentes. O Compose e scripts podem ser atualizados explicitamente; dados e volumes nunca são removidos pelo fluxo normal.

## Compose de produção

Serviços:

- `postgres`: imagem com versão explícita, volume persistente, sem porta pública, healthcheck `pg_isready`;
- `minio`: imagem com digest ou versão explícita, volume persistente, sem console público e sem API pública salvo decisão posterior;
- `keycloak`: imagem com versão explícita, banco persistente/compatível e healthcheck; não importar realm de desenvolvimento;
- `api`: imagem GHCR da release, `env_file` externo, dependências saudáveis, porta vinculada somente a `127.0.0.1` para Nginx.

Todos ficam em uma rede interna nomeada `sres_internal`. Usar `restart: unless-stopped` ou equivalente, volumes nomeados `sres_*_data` e limites somente depois de medir a VPS. O Compose não deve publicar PostgreSQL, MinIO ou console Keycloak para a Internet.

Keycloak pode ter uma publicação local exclusiva para o Nginx, caso seu issuer precise ser acessível por clientes externos. A API acessará `http://keycloak:8080` ou endereço interno conforme a configuração do realm; o issuer público precisa ser coerente com os tokens.

## Portas, Nginx e TLS

Portas públicas desejadas: 80, 443 e SSH. API, PostgreSQL, MinIO e Keycloak ficam internos ou vinculados ao loopback do host.

Nginx deve:

- encaminhar o domínio da API para a API;
- encaminhar o domínio do Keycloak para Keycloak quando necessário;
- enviar `Host`, `X-Real-IP`, `X-Forwarded-For` e `X-Forwarded-Proto`;
- usar timeouts compatíveis com upload/processamento assíncrono;
- definir `client_max_body_size` acima de 10 MB com margem operacional, sem transformar o limite do Nginx no único validador;
- validar configuração com `nginx -t` antes de reload;
- alterar somente arquivos de site do SReS e preservar backup em caso de erro.

TLS será terminado no Nginx com Certbot/Let's Encrypt ou mecanismo já administrado na VPS. HTTP deve redirecionar para HTTPS após certificado válido. Emissão/renovação não pode apagar configurações ou certificados existentes automaticamente.

Swagger em produção deve ser controlado por configuração: preferencialmente restrito ao domínio/rede administrativa ou desabilitado, mantendo possibilidade de `/v3/api-docs` quando uma integração autorizada exigir.

## PostgreSQL e Flyway

- volume persistente e credenciais exclusivas de produção;
- banco e usuário dedicados ao SReS;
- `SPRING_DATASOURCE_*` apontando para `postgres` na rede interna;
- Flyway continua a única autoridade para migrations;
- API só passa no health check depois de iniciar e validar/aplicar migrations;
- migration incompatível exige bloqueio/revisão antes de deploy;
- não haverá rollback automático de migration destrutiva;
- rollback da aplicação não restaura dump nem altera schema.

## MinIO

MinIO terá volume persistente, bucket privado `SRES_STORAGE_BUCKET`, credenciais em `app.env` e endpoint interno. O console não será publicado por padrão. A API é responsável por autorização e downloads.

Backup de objetos deve usar cópia/snapshot testado. Troca de credencial após inicialização do volume exige procedimento explícito do MinIO; editar somente o env não altera a credencial persistida.

## Keycloak

Produção terá realm, client, issuer e usuários próprios. O realm `sres-dev`, seus usuários de desenvolvimento, senhas e importação não podem ser promovidos automaticamente.

O bootstrap produtivo deve criar apenas estrutura sem secrets: realm/client/roles podem ser aplicados por export revisado ou procedimento administrativo; senhas, client secrets e usuários administrativos entram por secret store/manual seguro na VPS. Redirect URIs e origins devem ser limitados aos domínios decididos.

O issuer externo deve ser estável e igual ao valor aceito pela API em `SRES_KEYCLOAK_ISSUER`. A persistência do Keycloak deve ser respaldada pelo banco/backup escolhido; realm export não substitui backup de estado.

## Ollama e Telegram

Ollama não integra o Compose inicial. A API deve iniciar com:

```text
SRES_OLLAMA_ENABLED=false
SRES_OLLAMA_BASE_URL=<endereco externo quando ativado>
SRES_OLLAMA_MODEL=<modelo configurável>
```

`qwen3.5:4b` continua referência de desenvolvimento, não escolha produtiva. Produção exige benchmark futuro de qualidade, latência, memória, tokens/s, contexto, estabilidade e custo.

Telegram permanece:

```text
SRES_TELEGRAM_ENABLED=false
SRES_TELEGRAM_TOKEN=<somente na VPS quando ativado>
```

Token nunca vai ao GitHub, imagem ou logs. O limite atual de um poller por instância deve ser documentado; não haverá coordenação distribuída nem webhook nesta iniciativa.

## Arquivos de ambiente

`deploy/app.env.example` conterá apenas placeholders e nomes reais, incluindo:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sres
SPRING_DATASOURCE_USERNAME=CHANGE_ME
SPRING_DATASOURCE_PASSWORD=CHANGE_ME
SRES_KEYCLOAK_ISSUER=https://keycloak.<dominio-decidido>/realms/<realm-produtivo>
SRES_STORAGE_ENDPOINT=http://minio:9000
SRES_STORAGE_ACCESS_KEY=CHANGE_ME
SRES_STORAGE_SECRET_KEY=CHANGE_ME
SRES_STORAGE_BUCKET=sres-reports
SRES_OLLAMA_ENABLED=false
SRES_OLLAMA_BASE_URL=http://ollama.example.internal:11434
SRES_OLLAMA_MODEL=CHANGE_ME
SRES_TELEGRAM_ENABLED=false
SRES_TELEGRAM_TOKEN=CHANGE_ME
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
TZ=America/Sao_Paulo
```

O nome exato do token Telegram deve seguir a propriedade efetivamente implementada; o exemplo não deve inventar uma variável que a API não consome. Exemplos não são defaults de produção.

`deploy/deploy.env.example` separará mecanismo de deploy:

```text
SRES_API_IMAGE=ghcr.io/<owner>/sres-api
SRES_RELEASE=<sha-completa>
SRES_COMPOSE_PROJECT=sres
SRES_API_HEALTH_URL=http://127.0.0.1:<porta>/actuator/health
SRES_API_PORT=<loopback-porta>
SRES_KEYCLOAK_PORT=<loopback-porta-se-necessario>
SRES_HEALTH_TIMEOUT_SECONDS=120
SRES_PULL_POLICY=always
```

Não misturar secrets de aplicação com `deploy.env`.

## Deploy, health check e rollback

`sres-deploy <sha>` deve:

1. exigir exatamente uma SHA Git completa válida;
2. adquirir `flock` em `/var/lock/sres-deploy.lock`;
3. ler e validar envs restritos;
4. registrar a release atual;
5. fazer pull somente da imagem SHA;
6. executar `docker compose up -d` sem down destrutivo;
7. aguardar containers e `/actuator/health` com timeout/retries;
8. validar HTTP 200 e estado `UP`;
9. opcionalmente validar `/v3/api-docs` se habilitado e um endpoint protegido sem token retornando 401;
10. gravar `current-release` atomicamente e registrar timestamp/SHA;
11. em falha, tentar rollback apenas para SHA anterior válida;
12. liberar o lock.

Health check não pode aceitar somente `docker compose up`: deve verificar saúde do PostgreSQL, MinIO, Keycloak quando exposto pelo Compose, container da API e HTTP da API.

`sres-rollback <sha>` exige SHA válida, usa a mesma composição e health check, atualiza o registro somente após sucesso e nunca executa `flyway clean`, remove volume ou desfaz migration.

Guardar apenas `current-release`, `previous-release` ou histórico pequeno com timestamp e evento. Não criar um sistema de releases complexo.

## Bootstrap e instalação

`deploy/bootstrap.sh` e `deploy/scripts/install.sh` devem:

- exigir root apenas onde necessário;
- verificar Docker Compose, Nginx, curl e flock;
- criar `/opt/sres`, `/etc/sres`, usuário `sres-deploy` e scripts;
- instalar templates sem substituir envs existentes;
- instalar permissões `0600` para arquivos com secrets;
- criar sudoers restrito somente mediante arquivo revisado;
- não instalar secrets automaticamente;
- não executar `docker system prune`, `docker volume rm`, `down -v`, kill amplo ou remoção de diretórios genéricos.

O bootstrap pode baixar somente arquivos de uma referência revisada do GitHub, para uma pasta temporária segura, e deve remover essa cópia ao terminar. A implementação deve documentar como revisar/piná-la antes de executar.

## Backups

Antes de produção real, definir e testar:

- `pg_dump` periódico, retenção e restauração em banco isolado;
- cópia/snapshot dos objetos MinIO e teste de leitura;
- backup/export e dados do Keycloak conforme seu banco;
- armazenamento fora da VPS ou em destino com acesso restrito;
- monitoramento de sucesso, idade e espaço livre.

Deploy não é backup. Rollback de imagem não é recuperação de dados.

## Logs e recursos

Usar `docker logs` com rotação configurada no daemon/Compose. Não logar tokens, senhas, Authorization headers, client secrets, PDFs ou conteúdo integral de relatórios.

Não fixar `mem_limit`/CPU arbitrários antes de medir CPU, RAM, disco, I/O e concorrência da VPS. A primeira task deve coletar esses dados e definir limites conservadores ou justificar sua ausência.

## Segurança e ameaças

| Ameaça | Mitigação obrigatória |
|---|---|
| Secret no Git | `.env` fora do repositório, placeholders e revisão de diff |
| Secret em log | envs não impressos; logs sanitizados |
| SSH root | usuário dedicado e sudo restrito |
| Chave host falsa | `VPS_SSH_KNOWN_HOSTS` fixo e revisado |
| Pull GHCR amplo | token de leitura dedicado na VPS |
| Porta interna pública | bind interno/loopback e firewall |
| Docker socket | não expor socket ao container da API |
| Sudo arbitrário | comando único, SHA validada e script root instalado |
| Shell injection | regex de SHA, arrays Bash e sem eval |
| Rollback arbitrário | somente SHA válida e imagem definida no registry configurado |
| Migration incompatível | compatibilidade backward, backup e aprovação manual |
| Perda de dados | volumes persistentes e backups restauráveis |

## Critérios globais de aceite

- Compose de produção passa em `docker compose config` sem secrets reais.
- Imagem é publicada somente após CI verde e usa SHA imutável.
- Ambiente novo sobe API, PostgreSQL, MinIO e Keycloak com healthchecks.
- Ollama e Telegram desligados não impedem startup.
- API fica atrás do Nginx e somente portas necessárias são públicas.
- scripts rejeitam SHA inválida, lock concorrente e configurações ausentes.
- deploy bem-sucedido exige health HTTP real.
- falha de deploy tenta rollback seguro sem alterar migrations/volumes.
- `dev-down`/deploy não removem volumes.
- backups e restauração são documentados e testados antes de produção.
- documentação permite primeira instalação, operação, deploy, rollback e troubleshooting sem adivinhações.

## Fora do escopo

Kubernetes, k3s, GitLab, frontend, autoscaling, multi-region, Terraform complexo, Prometheus, Grafana, tracing externo, HA, múltiplas VPS, blue/green sofisticado, RAG, embeddings, CI/CD de frontend e plataforma produtiva de Ollama.

