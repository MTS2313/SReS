# Desenvolvimento local

Requisitos mínimos: Java 21, Git, Docker Engine com Docker Compose v2 e acesso à rede para o primeiro download do Maven Wrapper e das dependências.

Copie `.env.example` para `.env` e mantenha esses valores exclusivamente no ambiente local. Execute `scripts/dev-up.sh` para iniciar PostgreSQL, MinIO e Keycloak. Ollama e Telegram não são iniciados pelo Compose e permanecem desabilitados por padrão.

Use `scripts/dev-down.sh` para parar os serviços sem remover volumes. A remoção só é feita por `scripts/reset-dev.sh`, que exige o alvo literal `sres-dev` e confirmação explícita.

Para executar a API pelo Maven Wrapper:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Para construir e executar a imagem, o contexto é a raiz do monorepo:

```bash
docker build -f backend/Dockerfile -t sres-api:local .
docker run --rm --network host -e SPRING_PROFILES_ACTIVE=dev sres-api:local
```

O `backend/Dockerfile` usa build multi-stage, runtime Java 21 e usuário não-root. O `.dockerignore` exclui Git, artefatos locais, logs, `.env`, documentação e infraestrutura do contexto. Nenhuma credencial é embutida na imagem.

O schema é criado e evoluído exclusivamente por Flyway; o Hibernate usa `ddl-auto=validate`.

Erros HTTP usam Problem Details RFC 9457. O header `X-Correlation-ID` identifica uma requisição e aparece também nas respostas de erro. O Actuator local expõe somente `/actuator/health` e `/actuator/info`; credenciais, tokens e conteúdos de relatórios não devem ser enviados a logs.

O fluxo API usa tokens USER/ADMIN emitidos pelo realm `sres-dev` do Keycloak. O usuário comum consulta apenas a própria conta, cota e relatórios; operações administrativas exigem `ADMIN`. O Telegram permanece atrás de `SRES_TELEGRAM_ENABLED=false` e requer token externo somente quando habilitado. O processamento usa `SRES_OLLAMA_ENABLED=false` por padrão e o modelo de desenvolvimento configurável por `SRES_OLLAMA_MODEL` (atualmente `qwen3.5:4b`).

Antes de escolher modelo produtivo, execute benchmark comparando qualidade da saída, latência, memória, tokens por segundo, contexto, estabilidade e custo operacional. O smoke anterior do modelo local teve timeout; não há afirmação de modelo produtivo definitivo.
