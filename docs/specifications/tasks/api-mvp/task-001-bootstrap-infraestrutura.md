# TASK-001 — Bootstrap e infraestrutura executável

## Estado

approved

## Dependências

- Nenhuma.

## Objetivo

Concluir a fundação Java 21 compilável, modular e operacional dentro de `backend/`, preservando a raiz como interface compartilhada do futuro monorepo.

## Contexto da correção

A implementação inicial criou corretamente o projeto Spring, Compose, realm e scripts, mas posicionou `pom.xml`, `mvnw`, `.mvn/` e `src/` na raiz. A decisão DEC-023 exige mover somente o projeto Maven para `backend/` antes da TASK-002.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Arquitetura](../../../architecture/backend.md)
- [ADR Spring Modulith](../../../decisions/ADR-001-spring-modulith.md)

## Escopo da correção

- Criar `backend/` como diretório da aplicação.
- Mover de forma versionada `.mvn/`, `mvnw`, `pom.xml` e `src/` para `backend/`, preservando histórico e permissões.
- Manter `docs/`, `scripts/`, `compose.yaml`, `infra/`, `.env.example` e `.gitignore` na raiz.
- Alterar primeiro `scripts/validate.sh` para validar a estrutura esperada e produzir evidência Red antes da movimentação.
- Adaptar `scripts/test.sh` e `scripts/validate.sh` para executar o Maven Wrapper em `backend/`.
- Garantir que todos os scripts resolvam a raiz do repositório sem depender do diretório corrente.
- Preservar o comportamento de `scripts/dev-up.sh`, `scripts/dev-down.sh` e `scripts/reset-dev.sh` sobre o Compose da raiz.
- Criar `README.md` na raiz com a estrutura do monorepo, requisitos mínimos e comandos oficiais por `scripts/`.
- Atualizar a documentação viva somente onde o estado e os caminhos observados já forem sustentados pela implementação.
- Executar novamente todas as validações da fundação.

## Fora do escopo

- Entidades, endpoints ou regras de negócio.
- Integração real com Ollama ou Telegram.
- Criação de `frontend/` ou documentação de frontend.
- Dockerfile de produção.
- CI/CD, deploy ou reorganização adicional não exigida pela DEC-023.
- Alteração de versões ou dependências sem bloqueio comprovado.

## Passos verificáveis

1. Confirmar branch, estado local e arquivos implementados pela TASK-001.
2. Modificar `scripts/validate.sh` para exigir `backend/pom.xml`, `backend/mvnw`, `backend/.mvn/` e `backend/src/`, e proibir suas cópias na raiz.
3. Executar `scripts/validate.sh` antes de mover os arquivos e registrar a falha esperada como evidência Red.
4. Mover os quatro componentes do projeto Maven para `backend/`.
5. Adaptar os scripts de teste e validação, criar o README da raiz e corrigir referências documentais aplicáveis.
6. Executar `scripts/test.sh` e `scripts/validate.sh` até Green.
7. Subir e parar o ambiente pelos scripts da raiz para garantir que o Compose e `infra/` continuem funcionais.
8. Refatorar caminhos duplicados nos scripts sem quebrar as validações.

## Validação obrigatória

- `scripts/validate.sh` antes da movimentação — falha estrutural registrada como Red.
- `scripts/test.sh` após a movimentação — aplicação mínima e teste modular verdes em `backend/`.
- `scripts/validate.sh` após a movimentação — estrutura, build limpo, módulos e configuração válidos.
- `scripts/dev-up.sh` — PostgreSQL, MinIO e Keycloak saudáveis usando o Compose da raiz.
- Inspeção de `scripts/reset-dev.sh` — continua exigindo ambiente de desenvolvimento e confirmação explícita.
- `scripts/dev-down.sh` — encerra sem apagar volumes por padrão.
- `git status --short` e `git diff --summary` — movimentações reconhecidas e nenhuma duplicação estrutural.

## Critérios de conclusão

- [ ] `backend/pom.xml`, `backend/mvnw`, `backend/.mvn/` e `backend/src/` existem.
- [ ] Não existem `pom.xml`, `mvnw`, `.mvn/` ou `src/` na raiz.
- [ ] `docs/`, `scripts/`, `compose.yaml`, `infra/`, `.env.example` e `.gitignore` permanecem na raiz.
- [ ] Scripts funcionam quando invocados a partir da raiz e de outro diretório.
- [ ] Maven Wrapper compila com Java 21 dentro de `backend/`.
- [ ] Spring Modulith verifica os módulos sem violação.
- [ ] Flyway permanece habilitado e Hibernate não cria schema.
- [ ] Integrações opcionais não impedem a inicialização.
- [ ] Compose sobe os três serviços com healthchecks.
- [ ] README da raiz explica a estrutura e os comandos oficiais.
- [ ] Evidências Red, Green e Refactor foram fornecidas.
- [ ] `scripts/test.sh` e `scripts/validate.sh` terminam com sucesso.

## Contrato da resposta do agente

Informar resumo, arquivos movidos e alterados, evidência Red, Green e Refactor, scripts/testes com resultados, validações omitidas, desvios, riscos e estado recomendado.
