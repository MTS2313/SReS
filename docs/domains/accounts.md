# Contas

## Estado

**Planejado.**

## Objetivo

Representar os dados de negócio de uma pessoa que utiliza o SReS, sem duplicar a responsabilidade de autenticação do Keycloak.

## Responsabilidades

- associar os dados locais ao identificador da identidade no Keycloak;
- manter o estado operacional da conta;
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

## Identidade

O Keycloak será a autoridade para:

- credenciais;
- autenticação;
- emissão e validação de tokens;
- identidade base;
- roles de acesso.

A aplicação não armazenará senhas. Ela manterá uma referência estável ao usuário do Keycloak e somente os dados necessários ao domínio do SReS.

## Vinculação com Telegram

A vinculação será iniciada pela aplicação:

1. a conta autenticada solicita um código temporário;
2. a pessoa envia o código ao bot;
3. a API valida o código;
4. o identificador confirmado do Telegram é associado à conta.

O ID do Telegram não será aceito como prova de posse quando apenas digitado pelo usuário.

A duração do código, a cardinalidade dos vínculos e as regras de substituição de um vínculo existente ainda precisam ser definidas.

## Administração

Endpoints protegidos pela role `ADMIN` permitirão, no mínimo:

- consultar contas e seus estados;
- bloquear ou desbloquear uma conta;
- consultar consumo e relatórios;
- ajustar a cota de uma conta.

Uma interface administrativa não faz parte desta fase.
