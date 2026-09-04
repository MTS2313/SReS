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

## Executar a API

Para executar diretamente durante o desenvolvimento, use `SPRING_PROFILES_ACTIVE=dev` e inicie a aplicação pelo Maven Wrapper:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

O Ollama e o Telegram são opcionais e ficam desabilitados por padrão. As configurações externas usam `SRES_OLLAMA_*`, `SRES_TELEGRAM_*`, `SRES_KEYCLOAK_ISSUER` e `SPRING_DATASOURCE_*`; os valores de `.env.example` são somente para desenvolvimento.

## Imagem da API

O contexto de build é a raiz do monorepo, pois o Dockerfile está em `backend/` e copia somente o projeto backend:

```bash
docker build -f backend/Dockerfile -t sres-api:local .
docker run --rm --network host \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sres \
  -e SRES_STORAGE_ENDPOINT=http://localhost:9000 \
  -e SRES_KEYCLOAK_ISSUER=http://localhost:8080/realms/sres-dev \
  sres-api:local
```

Em ambientes sem `--network host`, substitua os endpoints pelos nomes DNS acessíveis a partir da rede Docker. A imagem não contém Maven, código-fonte, `.env`, senhas ou tokens; toda configuração é fornecida em runtime.
