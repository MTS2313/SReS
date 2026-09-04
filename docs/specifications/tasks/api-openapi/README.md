# Specification — Documentação OpenAPI da API SReS

## Estado

planned

## Contexto

O API MVP do SReS foi concluído e possui `springdoc-openapi-starter-webmvc-ui`, o endpoint `/v3/api-docs` e o Swagger UI disponíveis. A configuração atual expõe somente o título, a versão e o esquema global Bearer JWT. Os controllers ainda não fornecem um contrato OpenAPI suficientemente detalhado para consumo por frontend, mobile ou integrações sem leitura do código-fonte.

Esta specification define uma melhoria documental e de validação do contrato existente. Ela não cria endpoints, não altera regras de negócio e não reabre as tasks do API MVP.

## Objetivo

Tornar o Swagger/OpenAPI uma fonte utilizável para descobrir e consumir a API SReS, incluindo autenticação, autorização, requests, responses, schemas, erros, paginação, idempotência, upload, downloads e operações administrativas.

## Dependências

- API MVP concluída e validada.
- `springdoc-openapi-starter-webmvc-ui` já presente no `backend/pom.xml`.
- JWT Bearer validado pelo Keycloak através do Spring Security Resource Server.
- Contrato RFC 9457 implementado por `ApiExceptionHandler` e pelos handlers de autenticação/autorização.
- Controllers e DTOs existentes em `backend/src/main/java`.

## Diagnóstico obrigatório antes da implementação

Antes de alterar código, registrar uma comparação entre o documento gerado e o contrato real dos controllers:

- `AccountController`: `GET /api/v1/me`.
- `TelegramLinkController`: `POST /api/v1/me/telegram-link`.
- `ReportController`: `POST /api/v1/reports`, `GET /api/v1/reports`, `GET /api/v1/reports/{id}`, `GET /api/v1/reports/{id}/input` e `GET /api/v1/reports/{id}/output`.
- `UsageController`: `GET /api/v1/usage`, `GET /api/v1/usage/history`, criação e confirmação/liberação de reservas.
- `AdminAccountController`: consulta, bloqueio e desbloqueio de contas.
- `PlanController`: consulta, criação, alteração, ativação, inativação e definição de plano padrão.
- `UsageAdminController`: ajuste de quota, troca de plano, métricas, uso, histórico, custos e renovação.
- `AdminReportController`: consulta administrativa de relatórios.
- `AdminAuditController`: consulta administrativa de auditoria.

O diagnóstico deve distinguir:

- operações realmente existentes de operações apenas planejadas;
- códigos HTTP efetivamente retornados pelos controllers e handlers;
- campos públicos dos DTOs atuais;
- dados exclusivos de ADMIN, especialmente custos e auditoria;
- diferenças entre o contrato desejado e o documento produzido por `/v3/api-docs`.

## Escopo

### 1. Configuração global

Configurar o documento com:

- título `SReS API`;
- descrição curta do produto e do escopo atual;
- versão explícita da API, compatível com a versão efetivamente suportada;
- esquema HTTP Bearer JWT com `bearerFormat: JWT`;
- requisito de segurança somente nas operações protegidas;
- servidores configuráveis por ambiente, sem fixar uma URL produtiva inexistente;
- contato ou licença somente se houver dados oficiais no repositório.

O issuer do JWT deve ser explicado na descrição ou documentação operacional como Keycloak configurável por `SRES_KEYCLOAK_ISSUER`. A documentação não deve conter usuário, senha, token ou segredo real.

### 2. Swagger UI e exposição por ambiente

Garantir e testar:

- `/swagger-ui.html` redireciona ou responde conforme o comportamento do springdoc;
- `/swagger-ui/index.html` responde quando a documentação está habilitada;
- `/v3/api-docs` responde com JSON válido;
- a documentação pode ser habilitada/restringida ou desabilitada por configuração em produção;
- a autenticação da API não é enfraquecida para permitir o Swagger UI.

Os defaults de desenvolvimento podem manter o Swagger habilitado. A estratégia de produção deve ser configurável por propriedades `springdoc` ou configuração equivalente, sem afirmar que há proteção de rede que não exista.

### 3. Tags

Usar somente tags correspondentes a operações reais:

- `Account`;
- `Plans`;
- `Usage`;
- `Reports`;
- `Telegram`;
- `Administration`.

Operações com prefixo `/api/v1/admin` devem ser identificáveis visualmente como administrativas e declarar a exigência da role `ADMIN` na descrição ou extensão documental apropriada.

### 4. Operações

Adicionar `@Operation`, summaries curtos, descrições necessárias, parâmetros, headers, request bodies, respostas e tags nos limites dos controllers. A documentação deve explicar autorização sem duplicar regra de negócio dentro dos controllers.

O contrato deve documentar somente as operações listadas no diagnóstico. Não documentar contratação de plano, exclusão de relatório, processamento manual, webhook Telegram ou qualquer rota inexistente.

### 5. Schemas

Documentar os DTOs e estruturas públicas existentes com `@Schema` ou configuração equivalente:

- `AccountResponse` e `AccountStatus`;
- `PlanRequest`, `PlanUpdateRequest`, `PlanAssignmentRequest` e `PlanResponse`;
- `UsageSummary`, `LedgerResponse`, `ReservationResponse`, `QuotaAdjustmentRequest`, `MetricRequest` e `MetricResponse`;
- `ReportResponse` e `ReportPageResponse`;
- `TelegramLinkResponse`;
- `ProblemDetail`/`ProblemDetails` RFC 9457.

Registrar descrições, exemplos fictícios, UUID, `Instant`, `LocalDate`, `BigDecimal`, campos obrigatórios, limites de validação e enums reais. O schema de paginação deve explicar `content`, `page`, `size` e `totalElements`.

### 6. Segurança

Declarar um security scheme HTTP Bearer JWT que funcione com o botão `Authorize` do Swagger UI. Explicar:

- tokens são emitidos pelo Keycloak;
- `/api/v1/**` exige autenticação;
- `/api/v1/admin/**` exige `ROLE_ADMIN`;
- USER acessa apenas recursos próprios conforme o contrato;
- custos monetários e consultas administrativas não são públicos.

Não colocar tokens, credenciais de desenvolvimento ou valores de `client_secret` no OpenAPI.

### 7. Idempotência

Documentar `Idempotency-Key` nos endpoints que o exigem ou aceitam:

- criação de relatório;
- criação de reserva de quota;
- demais operações somente se o controller real utilizar o header.

Para cada ocorrência informar nome, formato textual, obrigatoriedade real, escopo da chave e comportamento de repetição. Não declarar obrigatoriedade onde o controller atual trata o header como opcional.

### 8. Entrada de relatório e PDF

Documentar `POST /api/v1/reports` como `multipart/form-data`, com:

- `type` textual e enum dos tipos suportados;
- `description` obrigatório;
- `file` opcional;
- PDF como tipo aceito;
- limite de 10 MB;
- limite de 50 páginas;
- resposta `202 Accepted` e header `Location`;
- estados possíveis do relatório.

O Swagger UI deve permitir selecionar o arquivo. Os limites documentados devem corresponder aos validadores reais da API.

### 9. Downloads

Documentar:

- `GET /api/v1/reports/{id}/input` como download do PDF de entrada quando existente;
- `GET /api/v1/reports/{id}/output` como download do Markdown/resultante quando disponível;
- `Content-Type` efetivo;
- autorização e ownership;
- respostas de inexistência e indisponibilidade.

Não documentar conteúdo binário ou arquivo que a aplicação não retorna.

### 10. Respostas HTTP

Documentar somente códigos realmente possíveis, conforme controller, validação, segurança e exception handler:

- `200 OK` para consultas, alterações e downloads;
- `201 Created` para criação de planos, métricas e reservas quando aplicável;
- `202 Accepted` para entrada assíncrona de relatórios;
- `204 No Content` somente se existir operação que efetivamente o retorne;
- `400 Bad Request` para validação e parâmetros inválidos;
- `401 Unauthorized` para ausência ou invalidade de autenticação;
- `403 Forbidden` para autorização insuficiente;
- `404 Not Found` para recursos inexistentes;
- `409 Conflict` para conflitos, conta bloqueada, cota e integridade;
- `502 Bad Gateway` para falha externa de armazenamento, quando aplicável;
- `500 Internal Server Error` para erro interno controlado.

Não adicionar `201`, `204` ou qualquer outro status apenas por convenção se ele não for produzido pela aplicação.

### 11. Problem Details RFC 9457

Criar um schema reutilizável, preferencialmente `ProblemDetails`, contendo:

- `type` como URI;
- `title`;
- `status`;
- `detail`;
- `instance`;
- `correlationId` como extensão.

Referenciar o schema em respostas de erro de validação, autenticação, autorização, bloqueio, cota, conflito, inexistência, armazenamento e erro interno. Não expor stack trace, SQL, classes internas, caminhos de filesystem ou segredos.

### 12. Exemplos

Adicionar exemplos fictícios e consistentes para:

- `GET /api/v1/me`;
- criação de relatório sem PDF;
- criação multipart com PDF;
- `ReportResponse` nos estados reais;
- `PlanResponse`;
- `UsageSummary`;
- `MetricResponse` administrativo;
- `TelegramLinkResponse`;
- resposta `ProblemDetails`.

Usar UUIDs, datas e textos claramente fictícios. Não usar tokens, subjects, senhas, IDs ou dados do ambiente local.

### 13. Administração

Separar pelas tags e descrições as operações administrativas:

- contas;
- planos;
- relatórios;
- quota e histórico;
- métricas;
- custos;
- auditoria;
- renovação.

Informar `ROLE_ADMIN`, paginação quando existente e a restrição de dados monetários. Não criar novas operações administrativas nesta specification.

## Validação automática obrigatória

Adicionar testes sem comparar o JSON inteiro. Os testes devem verificar:

- `/v3/api-docs` responde com HTTP 200;
- o documento pode ser desserializado como OpenAPI;
- existe security scheme HTTP Bearer JWT;
- existem os principais paths reais de account, plans, usage, reports, Telegram e administration;
- existem schemas de conta, plano, quota, relatório, vínculo Telegram e Problem Details;
- operações críticas possuem pelo menos uma resposta documentada;
- upload declara `multipart/form-data` e arquivo opcional;
- downloads declaram content types binários/textuais adequados;
- `Idempotency-Key` aparece onde utilizado;
- Swagger UI responde nos caminhos suportados.

O teste deve ser estável contra ordenação de JSON e permitir extensões documentais não relevantes.

## TDD

1. Criar primeiro os testes de contrato OpenAPI que exponham a documentação ausente ou incompleta.
2. Executar `scripts/test.sh` e registrar Red real, sem falha sintática artificial.
3. Implementar somente configuração, anotações, schemas e exemplos necessários.
4. Executar Green com a suíte existente preservada.
5. Refatorar documentação duplicada e manter os testes verdes.
6. Executar `scripts/test.sh`, `scripts/validate.sh`, `git diff --check`, `git status --short` e `git diff --stat`.

## Documentação viva

Atualizar somente após a implementação comprovada:

- `docs/architecture/api.md`, com os caminhos `/swagger-ui.html`, `/swagger-ui/index.html` e `/v3/api-docs`;
- `README.md` ou `docs/operations/local-development.md`, com comandos e configuração de desenvolvimento;
- documentação de segurança quando a estratégia de exposição por ambiente estiver implementada.

Não alterar o estado concluído do API MVP e não transformar frontend, CI/CD, deploy, benchmark produtivo do Ollama, webhook Telegram, múltiplos pollers, observabilidade externa, RAG ou embeddings em funcionalidades concluídas.

## Fora do escopo

- novos endpoints;
- alterações de regras de negócio;
- alterações de quotas, relatórios, processamento ou Telegram;
- frontend;
- deploy, CI/CD ou infraestrutura produtiva;
- escolha de modelo produtivo;
- mudança do mecanismo JWT/Keycloak;
- alteração de migrations;
- documentação de operações inexistentes.

## Critérios de conclusão

- [ ] Diagnóstico compara OpenAPI gerado com todos os controllers e DTOs reais.
- [ ] Configuração global contém título, descrição, versão e Bearer JWT.
- [ ] Swagger UI e `/v3/api-docs` são validados nos caminhos suportados.
- [ ] Tags organizam somente operações existentes.
- [ ] Operações críticas possuem summaries, descrições, parâmetros e respostas.
- [ ] Schemas, enums, limites, formatos e exemplos são utilizáveis.
- [ ] Autorização USER/ADMIN e ownership estão documentados.
- [ ] Idempotência, multipart PDF e downloads estão documentados corretamente.
- [ ] Problem Details RFC 9457 possui schema e referências de erro.
- [ ] Testes estruturais validam o contrato sem depender da ordenação do JSON.
- [ ] `scripts/test.sh`, `scripts/validate.sh` e `git diff --check` passam.
- [ ] Documentação viva informa os caminhos OpenAPI sem declarar funcionalidades inexistentes.

## Resultado recomendado

Após todos os critérios comprovados, recomendar `APPROVED_NEXT_TASK`. Caso a documentação gerada não corresponda ao comportamento real ou os testes estruturais falhem, recomendar `NEEDS_CORRECTION`. Usar `BLOCKED` somente quando uma dependência externa ou decisão não coberta impedir a validação.

## Contrato da resposta do agente

Informar situação encontrada, endpoints documentados, tags, schemas, segurança JWT, upload/download, Problem Details, exemplos, Swagger UI, testes adicionados, scripts executados, riscos, limitações e estado recomendado.
