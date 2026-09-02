# SReS — Documentação

## Estado do projeto

O repositório ainda não possui implementação. Toda a documentação deste primeiro ciclo descreve o estado **planejado** do produto e da API.

## Visão geral

SReS é um sistema de criação e processamento de relatórios assistidos por modelos executados no Ollama. O primeiro produto comercial será uma cota semanal de relatórios representada por planos internos.

O Telegram será a primeira interface funcional. A API será o núcleo do produto e deverá permitir que, após atingir uma estrutura mínima, uma interface seja desenvolvida separadamente — possivelmente um aplicativo mobile.

A ausência de frontend nesta fase não significa que o produto final será lançado sem interface. Apenas não haverá documentação ou implementação de frontend neste ciclo.

## Navegação

- [Visão do produto](product/overview.md)
- [Arquitetura do backend](architecture/backend.md)
- [Contrato da API](architecture/api.md)
- [Contas](domains/accounts.md)
- [Planos de relatórios](domains/plans.md)
- [Relatórios](domains/reports.md)
- [Cotas e custos](domains/quotas-and-costs.md)
- [Integração com Telegram](integrations/telegram.md)
- [Integração com Ollama](integrations/ollama.md)
- [Integração com MinIO](integrations/minio.md)
- [ADR-001: Spring Modulith](decisions/ADR-001-spring-modulith.md)

## Escopo inicial

- Java 21 e Maven;
- API em monólito modular com Spring Modulith;
- endpoints sob `/api/v1`, identificadores UUID e erros em Problem Details;
- rotas centradas em `/me`, `/reports`, `/usage` e `/admin`;
- criação assíncrona respondendo `202 Accepted`;
- criação por API autenticada ou Telegram usando o mesmo caso de uso;
- idempotência por `update_id` do Telegram e suporte a `Idempotency-Key` na API;
- autenticação e identidade gerenciadas pelo Keycloak;
- provisionamento local no primeiro acesso autenticado;
- contas individuais com plano de relatórios;
- atribuição administrativa de planos e plano padrão;
- bloqueio que impede novo consumo sem retirar acesso ao histórico;
- vinculação individual e segura com Telegram;
- bot interno usando long polling e estado de conversa persistido;
- três tipos fixos de relatório;
- entrada por texto e, opcionalmente, um PDF de até 10 MB e 50 páginas;
- extração textual com Apache PDFBox, sem OCR;
- processamento assíncrono com worker interno baseado no PostgreSQL;
- armazenamento privado de arquivos no MinIO;
- cota semanal renovada segunda-feira no fuso `America/Sao_Paulo`;
- ajustes de cota auditáveis;
- custos monetários técnicos visíveis somente para administradores;
- migrations com Flyway e validação do schema pelo Hibernate;
- logs estruturados, correlation ID e Spring Boot Actuator;
- documentação OpenAPI;
- endpoints administrativos mínimos protegidos pela role `ADMIN`.

## Fora do escopo deste ciclo

- frontend web ou mobile;
- documentação de interface;
- editor de agentes ou prompts;
- equipes e organizações;
- pagamentos, faturas e checkout;
- escolha de modelo pelo usuário;
- geração de relatório em PDF;
- múltiplos documentos por solicitação;
- exclusão de relatórios pelo usuário;
- filas externas;
- orquestração dinâmica entre agentes.
