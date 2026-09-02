# Specification source — API MVP do SReS

## Problema informado pelo usuário

### PRB-001 — Construir a API do SReS de ponta a ponta

- Contexto relatado: o repositório possui a documentação planejada do produto, mas ainda não possui aplicação, infraestrutura operacional ou testes.
- Impacto percebido: não existe backend capaz de autenticar contas, controlar planos e cotas, receber e processar relatórios, armazenar arquivos ou operar pelo Telegram.
- Resultado desejado: entregar uma API MVP executável, testada e documentada, pronta para receber uma interface em etapa posterior.

### PRB-002 — Preparar a estrutura para monorepo

- Contexto relatado: após a implementação inicial da TASK-001, o projeto Spring Boot ficou na raiz do repositório, que receberá outras aplicações futuramente.
- Impacto percebido: manter o build Java na raiz mistura a aplicação backend com a futura estrutura do monorepo e aumenta o custo de reorganização das próximas tasks.
- Resultado desejado: mover o projeto Spring para `backend/` antes da TASK-002, mantendo documentação e operações compartilhadas na raiz.

## Relação entre os problemas

As responsabilidades pertencem ao mesmo objetivo porque compõem um único fluxo verificável: uma conta autenticada possui plano e cota, solicita relatório por API ou Telegram, envia texto e PDF opcional, aguarda processamento assíncrono pelo Ollama e consulta ou recebe o resultado. A organização em `backend/` corrige a fundação dessa mesma iniciativa antes da implementação dos domínios; não constitui um produto independente.

## Contexto no repositório

O repositório MTS2313/SReS, branch main, já contém a implementação inicial da TASK-001. O projeto Spring, Maven Wrapper, código, Compose, infraestrutura e scripts foram criados na raiz. A fundação ainda não foi aceita porque a decisão posterior de monorepo exige uma correção estrutural antes da TASK-002.

Documentos de produto e arquitetura que permanecem como referência:

- docs/README.md
- docs/product/overview.md
- docs/architecture/backend.md
- docs/architecture/api.md
- docs/decisions/ADR-001-spring-modulith.md
- docs/domains/accounts.md
- docs/domains/plans.md
- docs/domains/quotas-and-costs.md
- docs/domains/reports.md
- docs/integrations/minio.md
- docs/integrations/ollama.md
- docs/integrations/telegram.md

## Evidências do estado atual

- `pom.xml`, `mvnw`, `.mvn/` e `src/` existem atualmente na raiz e formam o projeto Spring implementado pela TASK-001.
- `compose.yaml`, `infra/`, `.env.example` e `scripts/` existem na raiz e funcionam como recursos operacionais compartilhados.
- `scripts/test.sh` e `scripts/validate.sh` assumem atualmente que o Maven Wrapper e o código também estão na raiz.
- `docs/specifications` contém o pacote `api-mvp`; a TASK-001 volta para `needs_correction` até a reorganização ser validada.
- A documentação viva ainda descreve a fundação como planejada e deverá ser atualizada pela correção somente onde houver evidência de implementação.

## Decisões aprovadas

| ID | Decisão | Consequência |
|---|---|---|
| DEC-001 | Java 21, Maven e coordenadas br.com.sres:sres-api | Um único projeto Java com Maven Wrapper |
| DEC-002 | Monólito modular com Spring Modulith | Limites e dependências entre módulos devem ser testados |
| DEC-003 | PostgreSQL/JPA, Flyway e ddl-auto=validate | Flyway é a fonte do schema |
| DEC-004 | Compose local com PostgreSQL, MinIO e Keycloak | Ollama permanece externo e configurável |
| DEC-005 | Realm Keycloak importável com roles USER e ADMIN e usuários locais | Ambiente local não depende de configuração manual |
| DEC-006 | Telegram e Ollama condicionais | A aplicação inicia e os testes executam sem serviços externos reais |
| DEC-007 | TDD obrigatório em todas as tasks | Cada comportamento novo exige Red–Green–Refactor e evidência |
| DEC-008 | API sob /api/v1, UUID, paginação e Problem Details RFC 9457 | Contrato HTTP consistente e OpenAPI |
| DEC-009 | Conta local criada no primeiro token válido | Provisionamento idempotente ligado ao subject do Keycloak |
| DEC-010 | Plano Inicial com 10 relatórios semanais | Nova conta recebe o plano padrão |
| DEC-011 | Troca de plano imediata com reset integral auditável | Nova alocação não apaga consumo anterior |
| DEC-012 | Cota semanal em America/Sao_Paulo | Renovação regular segunda-feira às 00:00 |
| DEC-013 | Texto e um PDF opcional, até 10 MB e 50 páginas | PDFBox, sem OCR, imagens ou áudio |
| DEC-014 | MinIO privado com upload temporário e limpeza após 1 hora | Banco guarda metadados; arquivos ficam no objeto |
| DEC-015 | Processamento por worker no PostgreSQL | Sem RabbitMQ; estados PENDING, PROCESSING, COMPLETED e FAILED |
| DEC-016 | Um retry de geração e recuperação após 30 minutos | Falha definitiva devolve a reserva |
| DEC-017 | Um modelo Ollama compartilhado e prompts versionados | Modelo escolhido por benchmark e não pelo usuário |
| DEC-018 | Bot interno com long polling e conversa persistida | Um poller ativo; conversa expira em 30 minutos |
| DEC-019 | Idempotência por update_id e Idempotency-Key por 24 horas | Reenvio não duplica relatório nem cota |
| DEC-020 | Custos monetários somente para ADMIN | Usuário vê plano, cota e consumo |
| DEC-021 | Specifications na main e oito tasks sequenciais | Uma task por vez após revisão |
| DEC-022 | Frontend excluído | O OpenAPI será o contrato para cliente futuro |
| DEC-023 | Estrutura preparada para monorepo | `backend/` contém `.mvn/`, `mvnw`, `pom.xml` e `src/`; `docs/`, `scripts/`, `compose.yaml`, `infra/`, `.env.example` e `.gitignore` permanecem na raiz |

## Objetivos

- Criar a aplicação Spring Boot e seus limites modulares.
- Automatizar ambiente local, build, testes, validação e reset seguro.
- Implementar autenticação, contas, planos, cotas, custos e administração.
- Implementar entrada, armazenamento, processamento e consulta de relatórios.
- Implementar Telegram de ponta a ponta sem acoplar regras ao canal.
- Entregar segurança, observabilidade, documentação OpenAPI e empacotamento.
- Comprovar o fluxo por testes automatizados e validação operacional repetível.

## Não objetivos

- Frontend web ou mobile.
- Cobrança, checkout, assinatura, fatura ou compra avulsa.
- Equipes, organizações ou multitenancy.
- OCR, áudio, imagens, múltiplos PDFs ou PDF de saída.
- RabbitMQ, Redis, microsserviços, webhook Telegram ou vários pollers.
- Editor de prompts, escolha de modelo pelo usuário ou múltiplos agentes encadeados.
- Exclusão de relatório e política definitiva de retenção pública.
- CI/CD ou deploy em Kubernetes.

## Escopo funcional

### REQ-001 — Fundação executável

Criar em `backend/` o projeto Java 21 com Maven Wrapper, dependências compatíveis e pinadas, Spring Boot, Spring Modulith, Spring AI, MapStruct, JPA, Flyway, PostgreSQL, MinIO, Keycloak Resource Server, PDFBox, OpenAPI e Actuator.

### REQ-002 — Módulos e operações locais

Organizar em `backend/src/` os módulos de contas, planos, uso/cotas/custos, relatórios, armazenamento, Ollama, Telegram e administração. Manter na raiz o Compose local, a infraestrutura compartilhada e scripts seguros para subir, parar, testar, validar e resetar o ambiente.

### REQ-003 — Identidade e contas

Validar JWT do Keycloak, criar conta local idempotente pelo subject no primeiro acesso, expor /api/v1/me e aplicar estados ACTIVE e BLOCKED.

### REQ-004 — Planos

Manter planos ativos/inativos, Plano Inicial padrão com limite 10, atribuição administrativa e permanência de contas em plano inativado até migração.

### REQ-005 — Cotas e custos

Criar alocações semanais, reserva atômica, confirmação, devolução, renovação, ajustes auditados e reset imediato por troca de plano. Registrar tokens, duração, tentativas e custo estimado; ocultar valores monetários do usuário comum.

### REQ-006 — Armazenamento e PDF

Validar um PDF opcional de até 10 MB e 50 páginas, extrair texto com PDFBox, usar área temporária privada no MinIO, compensar falhas e limpar temporários com mais de 1 hora.

### REQ-007 — Entrada e consulta de relatórios

Aceitar multipart pela API, suportar Idempotency-Key por 24 horas, responder 202 com Location, listar e consultar somente recursos próprios e transmitir o Markdown por download autenticado.

### REQ-008 — Processamento assíncrono

Selecionar trabalho pendente com bloqueio seguro no PostgreSQL, impedir duplicação, executar um retry, recuperar PROCESSING após 30 minutos e preservar a alocação de origem.

### REQ-009 — Agentes Ollama

Versionar três prompts para EXECUTIVE_SUMMARY, DETAILED_ANALYSIS e STRUCTURED_EXTRACTION, usar um modelo configurável compartilhado, gerar Markdown e persistir métricas disponíveis.

### REQ-010 — Telegram

Vincular conta por código de uso único válido por 10 minutos, deduplicar update_id, persistir conversa por 30 minutos, criar relatório pelo mesmo caso de uso e entregar em até três tentativas: imediata, 1 minuto e 5 minutos.

### REQ-011 — Administração

Proteger operações com role ADMIN para contas, bloqueio, planos, atribuição, ajuste/reset de cota, relatórios, métricas e custos. Toda mudança de cota ou plano exige motivo e auditoria.

### REQ-012 — Contrato e observabilidade

Usar Problem Details, correlation ID, logs estruturados, OpenAPI e Actuator protegido. Integrações externas devem ser habilitadas por configuração.

### REQ-013 — Qualidade orientada por TDD

Toda task deve criar primeiro testes falhos para os comportamentos autorizados, implementar o mínimo para torná-los verdes, refatorar e executar scripts/test.sh e scripts/validate.sh.

### REQ-014 — Entrega operacional

Fornecer Dockerfile, configuração por ambiente, documentação de execução, evidência de fluxo ponta a ponta com externos simulados e smoke real opcional.

## Regras e limites

- Somente uma task ready pode ser executada.
- Não usar ddl-auto para criar ou alterar schema.
- Não armazenar senha, PDF ou Markdown completo no PostgreSQL.
- Não expor bucket, segredos, stack traces ou custos internos ao usuário.
- Conta bloqueada consulta e baixa histórico, mas não cria relatório nem vínculo.
- Idempotência e concorrência não podem consumir duas cotas.
- PostgreSQL e MinIO exigem compensação; não existe transação distribuída.
- Telegram não chama a própria API por HTTP.
- Long polling deve ter somente um executor ativo.
- Operações mutáveis e recorrentes usam scripts da raiz.
- Os scripts da raiz devem localizar o repositório de forma independente do diretório atual e executar o build em `backend/`.
- A validação estrutural deve falhar se `pom.xml`, `mvnw`, `.mvn/` ou `src/` voltarem a ocupar a raiz.
- reset-dev.sh deve exigir alvo de desenvolvimento e confirmação explícita.

## Casos de borda

- Repetir Idempotency-Key ou update_id retorna/reusa a operação original sem nova reserva.
- Duas solicitações simultâneas com uma unidade disponível aceitam apenas uma.
- Falha definitiva do Ollama libera reserva e marca FAILED.
- Reinício não perde PENDING; PROCESSING antigo volta a PENDING após o limite.
- Falha no Telegram não muda COMPLETED nem devolve cota.
- Troca de plano encerra a alocação anterior e cria saldo integral até a próxima segunda-feira.
- PDF inválido, grande ou com mais de 50 páginas falha antes da reserva.
- Conta bloqueada recebe erro de negócio ao criar e continua acessando resultado anterior.
- Plano inativo não aceita nova atribuição, mas não migra contas automaticamente.

## Impactos e compatibilidade

- Dados: banco novo criado integralmente por migrations Flyway.
- API: contrato novo /api/v1; não há compatibilidade legada.
- Interface: nenhum frontend nesta iniciativa.
- Integrações: Keycloak e MinIO locais; Ollama e Telegram externos opcionais por configuração.
- Migração: não há dados legados; somente seed local e Plano Inicial. A correção da TASK-001 move arquivos versionados sem alterar schema ou dados.

## Riscos e cuidados

- Versões incompatíveis de Spring Boot, Spring AI e Spring Modulith — piná-las e validar build e testes.
- Concorrência de cota e worker — testar com PostgreSQL real via Testcontainers.
- Divergência MinIO/PostgreSQL — compensação e testes de falha.
- Testes dependentes da internet — simular Telegram e Ollama por padrão.
- Long polling duplicado — condicionar ativação e documentar uma instância.
- Realm com credenciais locais — marcar como desenvolvimento e não reutilizar em produção.
- Modelo real insuficiente para PDF — benchmark é condição antes de ativação produtiva.
- Política de retenção ainda provisória — não apresentar o MVP como pronto para lançamento público sem revisão.

## Critérios de aceite

- [ ] AC-001 — Projeto em `backend/` compila em Java 21, módulos são verificados e o ambiente local sobe pelos scripts da raiz.
- [ ] AC-002 — Keycloak autentica USER e ADMIN e o provisionamento local é idempotente.
- [ ] AC-003 — Contas e planos cumprem estado, padrão, inativação e bloqueio.
- [ ] AC-004 — Cota resiste à concorrência, renova, ajusta e reseta com auditoria.
- [ ] AC-005 — PDF e objetos MinIO cumprem validação, privacidade, compensação e limpeza.
- [ ] AC-006 — API cria com 202, respeita idempotência, propriedade, paginação e download.
- [ ] AC-007 — Worker conclui, repete, falha, recupera e atualiza a alocação correta.
- [ ] AC-008 — Três agentes geram Markdown pelo adaptador Ollama e registram métricas.
- [ ] AC-009 — Telegram vincula, persiste conversa, deduplica e entrega com retries.
- [ ] AC-010 — ADMIN acessa funções internas e USER não vê custos ou recursos alheios.
- [ ] AC-011 — Problem Details, correlation ID, logs, OpenAPI e Actuator estão validados.
- [ ] AC-012 — Suíte TDD, integração e ponta a ponta passa por scripts repetíveis.
- [ ] AC-013 — Dockerfile e documentação permitem executar a API sem adivinhações.

## Definição de pronto

Todas as oito tasks foram revisadas e aceitas; scripts/test.sh e scripts/validate.sh terminam com sucesso; validações de módulos, migrations, segurança, concorrência, integrações e fluxo ponta a ponta possuem evidência; OpenAPI e documentação operacional refletem o comportamento; nenhum item fora do escopo foi implementado silenciosamente.
