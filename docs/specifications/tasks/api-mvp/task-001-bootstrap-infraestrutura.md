# TASK-001 — Bootstrap e infraestrutura executável

## Estado

ready

## Dependências

- Nenhuma.

## Objetivo

Criar uma fundação Java 21 compilável, modular e operacional, com ambiente local e scripts seguros, sobre a qual as demais tasks possam trabalhar.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Arquitetura](../../../architecture/backend.md)
- [ADR Spring Modulith](../../../decisions/ADR-001-spring-modulith.md)

## Escopo

- Criar projeto Maven br.com.sres:sres-api e Maven Wrapper.
- Selecionar e fixar versões estáveis e compatíveis de Spring Boot, Spring Modulith e Spring AI para Java 21; registrar a escolha.
- Adicionar dependências previstas pela specification sem implementar domínios.
- Criar aplicação mínima e estrutura modular inicial para contas, planos, uso, relatórios, armazenamento, Ollama, Telegram e administração.
- Criar teste de verificação dos módulos e dependências permitidas.
- Configurar perfis e propriedades tipadas, com Telegram e Ollama desabilitados por padrão.
- Configurar Flyway e Hibernate validate sem usar criação automática de schema.
- Criar compose.yaml para PostgreSQL, MinIO e Keycloak com healthchecks e volumes nomeados.
- Criar realm de desenvolvimento importável, client da API, roles USER e ADMIN e usuários locais claramente não produtivos.
- Criar .env.example sem segredos reais.
- Criar scripts/dev-up.sh, dev-down.sh, test.sh, validate.sh e reset-dev.sh.
- Documentar requisitos locais mínimos.

## Fora do escopo

- Entidades e endpoints de negócio.
- Integração real com Ollama ou Telegram.
- Dockerfile de produção.
- CI/CD, frontend e deploy.

## Passos verificáveis

1. Criar o harness mínimo de build e testes.
2. Escrever teste inicialmente falho que expresse a arquitetura modular esperada.
3. Registrar a falha Red antes de completar os módulos.
4. Implementar o mínimo para o teste modular ficar Green.
5. Refatorar a estrutura mantendo o teste verde.
6. Criar Compose, realm e scripts com proteções.
7. Subir o ambiente por script e verificar saúde.
8. Executar testes e validação pelo contrato de scripts.

## Validação obrigatória

- scripts/dev-up.sh — PostgreSQL, MinIO e Keycloak saudáveis.
- scripts/test.sh — aplicação mínima e teste modular verdes.
- scripts/validate.sh — build limpo, módulos verificados e configuração válida.
- Inspeção de scripts/reset-dev.sh — exige ambiente de desenvolvimento e confirmação explícita.
- scripts/dev-down.sh — encerra sem apagar volumes por padrão.

## Critérios de conclusão

- [ ] Maven Wrapper compila com Java 21.
- [ ] Versões estão fixadas e a compatibilidade foi registrada.
- [ ] Spring Modulith verifica os módulos sem violação.
- [ ] Flyway está habilitado e Hibernate não cria schema.
- [ ] Integrações opcionais não impedem a inicialização.
- [ ] Compose sobe os três serviços com healthchecks.
- [ ] Realm local contém client, roles e usuários de desenvolvimento.
- [ ] Scripts são executáveis, limitados e documentados.
- [ ] Evidências Red, Green e refatoração foram fornecidas.
- [ ] scripts/test.sh e scripts/validate.sh terminam com sucesso.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência Red, scripts/testes com resultados, validações omitidas, decisões de versões, desvios, riscos e estado recomendado.
