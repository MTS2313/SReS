# TASK-005 — Nginx/TLS e preparação da VPS

## Estado

ready

## Objetivo

Preparar a entrada pública da VPS com Nginx, domínios decididos, HTTPS e limites coerentes com a API, sem expor serviços internos.

## Escopo

- templates/check/apply do Nginx sob `deploy/nginx/`;
- reverse proxy da API e Keycloak quando necessário;
- forwarded headers, timeouts e `client_max_body_size`;
- `nginx -t`, backup/restauração e reload seguro;
- Certbot/Let's Encrypt documentado e redirect HTTP→HTTPS;
- firewall/portas e checklist de DNS;
- Swagger configurável/restrito em produção.

## Fora do escopo

Frontend, CDN, WAF completo, observabilidade externa, emissão destrutiva de certificados e domínio inventado.

## TDD/validação

Testar domínios/portas inválidos, configuração gerada, `nginx -t`, upload de 10 MB com margem, headers e ausência de rotas internas públicas. Usar host/virtual host de teste explicitamente identificado.

## Critérios de conclusão

- somente 80/443/SSH são públicos;
- API chega pelo Nginx;
- PostgreSQL, MinIO e console não são públicos;
- Keycloak possui issuer externo coerente quando exposto;
- TLS é renovável sem apagar configuração existente;
- Swagger não fica público por acidente.

## Resposta esperada

Relatar topologia, domínios usados, testes Nginx/TLS, portas, limitações de DNS/certificados e estado recomendado.
