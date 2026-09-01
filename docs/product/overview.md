# Visão do produto

## Estado

**Planejado.** Ainda não existe implementação no repositório.

## Objetivo

SReS permitirá que uma pessoa solicite relatórios pelo Telegram, envie uma orientação em texto e, opcionalmente, um PDF, e receba o resultado processado por um modelo executado no Ollama.

O produto inicial comercializa capacidade de processamento na forma de uma cota semanal de relatórios.

## Experiência inicial

1. A pessoa cria uma conta por uma interface cliente futura.
2. A identidade e as credenciais são gerenciadas pelo Keycloak.
3. A pessoa gera um código temporário de vinculação.
4. O código é enviado ao bot do Telegram.
5. Pelo bot, a pessoa escolhe o tipo de relatório, envia a descrição e pode anexar um PDF.
6. A API valida e reserva uma unidade da cota.
7. O relatório é processado de forma assíncrona.
8. O Telegram recebe um resumo e um arquivo Markdown com o conteúdo completo.
9. Em falha técnica definitiva, a unidade reservada retorna para a cota disponível.

Enquanto não houver uma interface própria, a criação de conta, a geração do código de vínculo e as operações administrativas precisarão ser acessíveis por API ou por uma ferramenta técnica. A experiência definitiva dessa etapa ainda não foi decidida.

## Catálogo inicial

O catálogo terá três tipos fixos:

- resumo executivo;
- análise detalhada;
- extração estruturada.

Cada tipo corresponde a uma configuração interna e a um prompt controlado pelo sistema. Usuários não poderão criar agentes, editar prompts ou montar fluxos no MVP.

## Limites do MVP

Uma solicitação aceitará:

- uma descrição textual;
- no máximo um PDF opcional.

Não fazem parte desta versão:

- imagens e áudio;
- múltiplos PDFs por solicitação;
- relatórios personalizados pelo usuário;
- geração de PDF como saída;
- equipes ou organizações;
- cobrança e pagamento integrados.

## Estratégia de interfaces

A API é o núcleo do sistema. O Telegram é a primeira interface funcional, mas não será necessariamente a interface principal do produto lançado.

Um frontend será desenvolvido em uma etapa separada quando a API atingir uma estrutura mínima. A tecnologia e o formato desse cliente — web ou mobile — permanecem em aberto e não fazem parte da documentação atual.
