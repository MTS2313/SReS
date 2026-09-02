# Specification execution — API MVP do SReS

## Estratégia

Construir o sistema por fatias dependentes e revisáveis. A fundação operacional vem primeiro; domínios centrais antecedem relatórios; integrações externas usam portas e adaptadores condicionais; a validação ponta a ponta ocorre somente depois de cada fatia ser aceita.

TDD é obrigatório. Em cada task, o agente deve:

1. preparar somente o mínimo de harness necessário;
2. escrever teste que falha pelo comportamento ainda ausente;
3. registrar a evidência do estado Red;
4. implementar o mínimo para Green;
5. refatorar sem quebrar a suíte;
6. executar scripts/test.sh e scripts/validate.sh;
7. não avançar para a task seguinte.

## Preparação obrigatória

1. Ler instruções do repositório, se existirem.
2. Inspecionar a pasta raiz `scripts/`; ela permanece como interface operacional do monorepo.
3. Confirmar branch main, estado local limpo e task ready.
4. Ler tasks/api-mvp/README.md, esta execução e spec-source/api-mvp/README.md.
5. Conferir docs vivos relacionados à task.
6. Interromper se código observado contradizer a specification.

## Áreas afetadas

| Área/caminho | Papel | Cuidado |
|---|---|---|
| backend/pom.xml e backend/.mvn/ | build e versões da API | Pinagem compatível com Java 21 |
| backend/src/main/java/br/com/sres | aplicação modular | Sem acesso cruzado a internals |
| backend/src/main/resources/db/migration | schema e seed | Flyway como única fonte |
| backend/src/main/resources/prompts | prompts versionados | Três tipos fixos |
| backend/src/test | TDD e integração | Externos simulados por padrão |
| scripts/ | interface operacional do monorepo | Executar build e validações em `backend/` |
| compose.yaml e infra/ | ambiente local compartilhado e realm | Permanecem na raiz; sem Ollama obrigatório |
| docs/ | arquitetura, OpenAPI e operação | Permanecem na raiz; manter Planned/Current coerente |
| backend/Dockerfile | empacotamento final da API | Criado na TASK-008 |

A estrutura aprovada é uma raiz de monorepo com `backend/` para o projeto Spring. Não mover `docs/`, `scripts/`, `compose.yaml`, `infra/`, `.env.example` ou `.gitignore` para dentro do backend.

## Fases de execução

### Fase 1 — Fundação

- Requisitos: REQ-001, REQ-002, REQ-013.
- Task: TASK-001.
- Resultado: projeto em `backend/` compila, módulos existem, Compose e scripts da raiz funcionam, realm local é importável.
- Interrupção: incompatibilidade de versões ou ambiente incapaz de executar testes.

### Fase 2 — Identidade, contas e planos

- Requisitos: REQ-003, REQ-004, parte de REQ-011.
- Task: TASK-002.
- Resultado: JWT, provisionamento, /me, estados, plano padrão e administração básica.
- Interrupção: claims/roles do realm divergirem da configuração da API.

### Fase 3 — Cotas, custos e administração

- Requisitos: REQ-005, REQ-011.
- Task: TASK-003.
- Resultado: alocações, concorrência, renovação, ajustes, troca de plano e visibilidade.
- Interrupção: testes com PostgreSQL real não demonstrarem atomicidade.

### Fase 4 — Entrada e armazenamento

- Requisitos: REQ-006, REQ-007.
- Task: TASK-004.
- Resultado: API multipart, PDFBox, MinIO, idempotência, consulta e download.
- Interrupção: compensação não evitar órfãos ou reserva indevida.

### Fase 5 — Processamento e Ollama

- Requisitos: REQ-008, REQ-009.
- Task: TASK-005.
- Resultado: worker persistente, adaptador Spring AI/Ollama, prompts, retries, métricas e eventos.
- Interrupção: worker permitir dupla seleção ou corromper alocação.

### Fase 6 — Telegram

- Requisitos: REQ-010.
- Task: TASK-006.
- Resultado: long polling condicional, vínculo, conversa, deduplicação e entrega.
- Interrupção: bot depender de HTTP interno ou teste exigir Telegram real.

### Fase 7 — Robustez transversal

- Requisitos: REQ-011, REQ-012, REQ-013.
- Task: TASK-007.
- Resultado: Problem Details, autorização completa, correlation ID, logs, Actuator, jobs de limpeza e testes negativos.
- Interrupção: endpoints operacionais ou dados internos ficarem expostos.

### Fase 8 — Prova ponta a ponta

- Requisitos: REQ-014 e critérios globais.
- Task: TASK-008.
- Resultado: Dockerfile, documentação e fluxo completo validado com externos simulados.
- Interrupção: qualquer task anterior não estiver aceita.

## Política de scripts

A pasta `scripts/` foi criada pela implementação inicial da TASK-001 e permanece na raiz como interface operacional do monorepo. A correção deve adaptar os scripts de build e validação para o projeto em `backend/`.

Scripts obrigatórios após a TASK-001:

- scripts/dev-up.sh — subir PostgreSQL, MinIO e Keycloak e verificar saúde.
- scripts/dev-down.sh — parar o ambiente sem apagar dados por padrão.
- scripts/test.sh — executar testes determinísticos no projeto `backend/`.
- scripts/validate.sh — validar a estrutura do monorepo e executar testes, verificação modular, migrations e validações estáticas no projeto `backend/`.
- scripts/reset-dev.sh — reset somente de recursos de desenvolvimento com confirmação explícita.

Comandos diretos são permitidos somente para leitura, git status/diff, movimentação versionada autorizada e diagnóstico seguro. Mudanças de ambiente, banco, dependências e infraestrutura devem passar pelos scripts.

Para a correção estrutural da TASK-001, a evidência Red é `scripts/validate.sh` falhando ao exigir a nova estrutura antes da movimentação. O estado Green exige o mesmo script e `scripts/test.sh` concluindo com o projeto dentro de `backend/`.

## Testes e validações

| Validação | Script | Evidência |
|---|---|---|
| Testes unitários e integração | scripts/test.sh | Todos verdes |
| Limites Spring Modulith | scripts/validate.sh | Sem dependência proibida |
| Flyway e JPA validate | scripts/validate.sh | Schema limpo migra e valida |
| Segurança HTTP | scripts/test.sh | USER/ADMIN/propriedade testados |
| Concorrência de cota e worker | scripts/test.sh | Testcontainers PostgreSQL |
| MinIO e compensação | scripts/test.sh | Testcontainers ou serviço compatível controlado |
| Ollama/Telegram | scripts/test.sh | Simuladores determinísticos |
| Ambiente local | scripts/dev-up.sh | Serviços saudáveis |
| Reset | scripts/reset-dev.sh com confirmação | Somente alvo dev removido |
| Ponta a ponta | scripts/validate.sh | Fluxo completo comprovado |

## Dados, migração e recuperação

O banco começa vazio. A TASK-001 estabelece Flyway e o seed do Plano Inicial/realm local nas fases correspondentes. Toda evolução posterior adiciona migration, nunca edita migration já aplicada após aceitação.

Testes de integração usam recursos isolados. reset-dev.sh deve validar nome do projeto/ambiente, exigir confirmação explícita e nunca usar alvos amplos.

MinIO usa prefixo temporário e compensação. Operações falhas devem ser repetíveis ou recuperáveis sem duplicar cota.

## Restrições

- Não implementar frontend, billing, microsserviços, RabbitMQ, Redis ou OCR.
- Não tornar Ollama ou Telegram requisito para iniciar a API.
- Não usar credenciais locais fora de desenvolvimento.
- Não pular o teste Red por já conhecer a solução.
- Não usar mocks onde concorrência ou semântica PostgreSQL são parte do requisito.
- Não marcar task done sem evidências dos scripts.
- Não deixar cópias de `pom.xml`, `mvnw`, `.mvn/` ou `src/` na raiz.
- Não mover a infraestrutura ou documentação compartilhada para `backend/`.
- Não alterar docs vivos para esconder divergência; interromper e reportar.

## Rastreabilidade

| Requisitos | Fase | Task | Aceite |
|---|---|---|---|
| REQ-001, REQ-002, REQ-013 | 1 | TASK-001 | AC-001, AC-012 |
| REQ-003, REQ-004 | 2 | TASK-002 | AC-002, AC-003 |
| REQ-005, REQ-011 | 3 | TASK-003 | AC-004, AC-010 |
| REQ-006, REQ-007 | 4 | TASK-004 | AC-005, AC-006 |
| REQ-008, REQ-009 | 5 | TASK-005 | AC-007, AC-008 |
| REQ-010 | 6 | TASK-006 | AC-009 |
| REQ-011, REQ-012, REQ-013 | 7 | TASK-007 | AC-010, AC-011, AC-012 |
| REQ-014 | 8 | TASK-008 | AC-012, AC-013 |
