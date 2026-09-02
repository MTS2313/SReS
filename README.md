# SReS

O SReS é uma API para criação e processamento assistido de relatórios. Este repositório está organizado como um monorepo para receber outras aplicações futuramente.

## Estrutura

- `backend/` — aplicação Spring Boot Java 21, com Maven Wrapper e módulos Spring Modulith.
- `docs/` — documentação viva, arquitetura e specifications.
- `scripts/` — interface operacional compartilhada do monorepo.
- `infra/` e `compose.yaml` — infraestrutura local compartilhada (PostgreSQL, MinIO e Keycloak).

Frontend ainda não faz parte desta fase.

## Requisitos

Java 21, Docker e Docker Compose v2. O Maven é obtido pelo Maven Wrapper na primeira execução.

## Comandos oficiais

```bash
scripts/dev-up.sh       # sobe e aguarda PostgreSQL, MinIO e Keycloak
scripts/test.sh         # executa a suíte do backend
scripts/validate.sh     # valida estrutura, configuração, módulos e build
scripts/dev-down.sh     # para serviços sem apagar volumes
```

Para um reset destrutivo exclusivamente local, use `scripts/reset-dev.sh sres-dev` e confirme explicitamente quando solicitado.
