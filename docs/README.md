# SReS — Documentação

## Estado do projeto

O repositório ainda não possui implementação. Toda a documentação deste primeiro ciclo descreve o estado **planejado** do produto e da API.

## Visão geral

SReS é um sistema de criação e processamento de relatórios assistidos por modelos executados no Ollama. O primeiro produto comercial será uma cota semanal de relatórios.

O Telegram será a primeira interface funcional. A API será o núcleo do produto e deverá permitir que, após atingir uma estrutura mínima, uma interface seja desenvolvida separadamente — possivelmente um aplicativo mobile.

A ausência de frontend nesta fase não significa que o produto final será lançado sem interface. Apenas não haverá documentação ou implementação de frontend neste ciclo.

## Navegação

- [Visão do produto](product/overview.md)
- [Arquitetura do backend](architecture/backend.md)
- [Contas](domains/accounts.md)
- [Relatórios](domains/reports.md)
- [Cotas e custos](domains/quotas-and-costs.md)
- [Integração com Telegram](integrations/telegram.md)
- [Integração com Ollama](integrations/ollama.md)

## Escopo inicial

- autenticação e identidade gerenciadas pelo Keycloak;
- vinculação segura entre conta e usuário do Telegram;
- três tipos fixos de relatório;
- entrada por texto e, opcionalmente, um arquivo PDF;
- processamento assíncrono com worker interno;
- armazenamento de arquivos no MinIO;
- cota semanal por conta;
- registro de tokens, duração e custo técnico estimado;
- endpoints administrativos mínimos protegidos pela role `ADMIN`.

## Fora do escopo deste ciclo

- frontend web ou mobile;
- documentação de interface;
- editor de agentes ou prompts;
- equipes e organizações;
- pagamentos, faturas e checkout;
- geração de relatório em PDF;
- múltiplos documentos por solicitação;
- filas externas;
- orquestração dinâmica entre agentes.
