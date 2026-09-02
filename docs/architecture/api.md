# Contrato da API

## Estado

**Planejado.** Não existe implementação no repositório.

## Objetivo

Oferecer um contrato HTTP estável para operações técnicas, futura interface web ou mobile e demais clientes autorizados, sem acoplar os casos de uso ao Telegram.

O bot está dentro da mesma aplicação e usa os serviços de aplicação diretamente. Ele não fará chamadas HTTP contra a própria API.

## Convenções

- prefixo `/api/v1`;
- JSON para recursos e respostas estruturadas;
- `multipart/form-data` para criação de relatório com arquivo;
- UUID como identificador externo;
- paginação por `page`, `size` e `sort`;
- erros no formato Problem Details da RFC 9457;
- documentação OpenAPI desde a primeira versão;
- autenticação por token emitido pelo Keycloak.

## Organização inicial

Os recursos serão centrados na conta autenticada:

- `/api/v1/me` para conta e plano atuais;
- `/api/v1/me/telegram-link` para vínculo com Telegram;
- `/api/v1/reports` para criação e consulta de relatórios;
- `/api/v1/usage` para cota e consumo próprios;
- `/api/v1/admin/*` para contas, planos, custos e demais operações administrativas.

O usuário comum não informará o próprio identificador de conta nas rotas. A identidade será derivada do token.

A definição de sub-rotas e verbos será consolidada na specification de implementação e publicada no OpenAPI.

## Criação de relatório

O endpoint autenticado de criação receberá `multipart/form-data` com:

- código fixo do tipo de relatório;
- descrição textual;
- um PDF opcional.

O PDF aceita até 10 MB e 50 páginas. Validações de formato, tamanho e páginas devem acontecer antes da reserva de cota.

Quando a solicitação de uma conta ativa for aceita, a API responderá:

- HTTP `202 Accepted`;
- identificador UUID do relatório;
- estado `PENDING`;
- header `Location` apontando para o recurso de consulta.

Conta bloqueada receberá erro de autorização de negócio consistente e não reservará cota.

## Idempotência

A criação pela API aceitará o header `Idempotency-Key`.

- repetir a mesma chave dentro do escopo da mesma conta não cria outro relatório;
- a resposta deve referenciar o resultado da primeira solicitação aceita;
- a chave de uma conta não interfere em outra;
- a chave será reconhecida por 24 horas;
- o uso do header será suportado, mas não obrigatório no MVP.

No Telegram, o `update_id` será tratado como identificador único de entrada.

## Consulta

Usuários poderão consultar:

- conta e plano atuais;
- vínculo com Telegram;
- relatório individual;
- histórico paginado de relatórios;
- estado do processamento;
- cota semanal;
- histórico próprio de consumo.

Administradores poderão consultar adicionalmente:

- planos;
- contas e seus estados;
- tokens e duração;
- custos monetários estimados;
- ajustes de cota;
- métricas operacionais.

Custos técnicos monetários não serão expostos ao usuário comum.

## Resultado e arquivos

O conteúdo completo continuará no MinIO. O cliente fará download por endpoint autenticado da API, que validará conta e autorização antes de transmitir o arquivo.

Uma conta bloqueada poderá continuar consultando e baixando os próprios resultados existentes.

Não haverá URL pública permanente, armazenamento do Markdown completo no PostgreSQL ou endpoint de exclusão no MVP.

## Autorização

- usuários acessam somente os próprios recursos;
- endpoints administrativos exigem a role `ADMIN`;
- o identificador recebido na URL nunca substitui a verificação de propriedade;
- conta bloqueada não cria relatório nem vínculo;
- o bot usa o vínculo confirmado para localizar a conta, mas executa as mesmas regras do caso de uso.

## Erros e rastreabilidade

Cada requisição terá um correlation ID. O identificador será propagado nos logs e poderá ser retornado em respostas de erro para facilitar diagnóstico, sem expor detalhes internos sensíveis.

Erros de validação, autenticação, autorização, bloqueio, cota esgotada, recurso inexistente e conflito devem ser representados de forma consistente por Problem Details. O catálogo definitivo de códigos ainda será definido.

## OpenAPI

O OpenAPI será o contrato técnico de integração com o futuro frontend. Ele documentará campos, validações, autenticação, estados e erros, mas não decisões visuais ou tecnologia da interface.
