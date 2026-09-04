# TASK-003 — CI backend e publicação GHCR

## Estado

approved

## Objetivo

Criar CI do backend e pipeline de publicação de imagem imutável no GHCR, garantindo que código sem validação não seja publicado.

## Escopo

- `.github/workflows/backend.yml` para PR/push relevante;
- Java 21 e cache Maven;
- `scripts/test.sh` e `scripts/validate.sh` sem duplicação desnecessária;
- build multi-stage a partir do contexto correto da raiz;
- login GHCR com `GITHUB_TOKEN` e `packages: write`;
- tag `ghcr.io/<owner>/sres-api:<sha>`.

## Fora do escopo

SSH de produção, workflow de deploy, secrets da VPS, Nginx/TLS e frontend.

## TDD/validação

Criar validações falhas para path filters, permissões, versão Java, ausência de `latest` como release e ordem test-before-push. Validar YAML, Actions, build local e inspeção de tags/configuração.

## Critérios de conclusão

- PR executa testes e validações relevantes;
- publicação ocorre somente após sucesso;
- imagem usa SHA completa e não inclui secrets;
- permissions mínimas estão declaradas;
- alterações exclusivamente documentais não publicam imagem;
- workflow é auditável e não usa comandos SSH.

## Resposta esperada

Relatar checks locais, build, configuração do workflow, imagem criada quando permitido e limitações de execução no GitHub.
