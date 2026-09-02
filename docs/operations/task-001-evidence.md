# Evidências da TASK-001

- Red: `./mvnw -DskipTests=false test` falhou antes da implementação com erro de compilação porque `SresApplication` ainda não existia, exatamente como o teste modular exigia.
- Green: após criar somente a aplicação mínima e os oito marcadores de módulo, `./mvnw -B test` passou com 1 teste, 0 falhas e 0 erros.
- Refactor: a estrutura foi organizada em `package-info.java`, configuração YAML e scripts separados; o mesmo teste permaneceu verde.
- Operacional: `scripts/dev-up.sh` iniciou PostgreSQL, MinIO e Keycloak saudáveis; `scripts/dev-down.sh` parou-os sem remover os dois volumes nomeados.
