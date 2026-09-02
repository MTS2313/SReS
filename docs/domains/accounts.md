# Contas

## Estado

**Planejado.**

## Objetivo

Representar os dados de negócio de uma pessoa que utiliza o SReS, sem duplicar a responsabilidade de autenticação do Keycloak.

## Responsabilidades

- associar os dados locais ao identificador da identidade no Keycloak;
- provisionar o registro local no primeiro acesso autenticado;
- manter o estado operacional da conta;
- associar a conta ao seu plano de relatórios;
- manter o vínculo com o Telegram;
- relacionar a conta à sua cota e ao histórico de relatórios;
- permitir bloqueio administrativo.

## Limites

A primeira versão terá somente contas individuais.

Não haverá:

- organizações;
- equipes;
- convites;
- membros;
- permissões de negócio além da separação entre usuário e administrador.

## Identidade e provisionamento

O Keycloak será a autoridade para:

- credenciais;
- autenticação;
- emissão e validação de tokens;
- identidade base;
- roles de acesso.

A aplicação não armazenará senhas.

No primeiro acesso com token válido, a aplicação criará o registro local associado ao identificador estável do usuário no Keycloak. A criação será idempotente: requisições simultâneas ou repetidas não poderão criar contas duplicadas.

A nova conta receberá automaticamente o plano de relatórios definido como padrão.

## Estados operacionais

A conta poderá estar ativa ou bloqueada.

Uma conta bloqueada:

- continua podendo autenticar;
- pode consultar o próprio histórico;
- pode consultar relatórios já produzidos;
- pode baixar resultados existentes;
- não pode criar relatórios;
- não pode gerar novo código de vínculo com Telegram;
- não pode iniciar novo consumo de cota.

O bloqueio não apaga relatórios, arquivos, consumo, plano ou vínculo existente.

## Plano

Cada conta terá um plano de relatórios associado. A atribuição e a troca serão realizadas por administrador enquanto não existir contratação pelo próprio usuário.

A conta também poderá receber ajustes auditáveis de cota sem alterar o plano.

## Relação com Telegram

A relação será individual:

- uma conta poderá ter somente um usuário do Telegram vinculado;
- um usuário do Telegram poderá estar vinculado a somente uma conta;
- a troca exigirá desvincular o vínculo anterior e gerar um novo código.

## Vinculação com Telegram

1. A conta ativa e autenticada solicita um código.
2. O código permanece válido por 10 minutos e aceita um único uso.
3. A pessoa envia o código ao bot.
4. A aplicação valida código, prazo, uso e disponibilidade do vínculo.
5. O identificador confirmado do Telegram é associado à conta.
6. O código é invalidado.

O ID do Telegram não será aceito como prova de posse quando apenas digitado pelo usuário.

## Administração

Endpoints protegidos pela role `ADMIN` permitirão, no mínimo:

- consultar contas e seus estados;
- bloquear ou desbloquear uma conta;
- consultar consumo e relatórios;
- atribuir plano;
- ajustar a cota de uma conta.

Uma interface administrativa não faz parte desta fase.
