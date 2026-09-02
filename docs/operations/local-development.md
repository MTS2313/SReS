# Desenvolvimento local

Requisitos mínimos: Java 21, Git, Docker Engine com Docker Compose v2 e acesso à rede para o primeiro download do Maven Wrapper e das dependências.

Copie `.env.example` para `.env` e mantenha esses valores exclusivamente no ambiente local. Execute `scripts/dev-up.sh` para iniciar PostgreSQL, MinIO e Keycloak. Ollama e Telegram não são iniciados pelo Compose e permanecem desabilitados por padrão.

Use `scripts/dev-down.sh` para parar os serviços sem remover volumes. A remoção só é feita por `scripts/reset-dev.sh`, que exige o alvo literal `sres-dev` e confirmação explícita.

O schema é criado e evoluído exclusivamente por Flyway; o Hibernate usa `ddl-auto=validate`.
