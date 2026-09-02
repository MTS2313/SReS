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
- paginação para coleções;
- erros no formato Problem Details da RFC 9457;
- documentação OpenAPI desde a primeira versão;
- autenticação por token emitido pelo Keycloak.

## Criação de relatório

O endpoint autenticado de criação receberá `multipart/form-data` com:

- tipo fixo de relatório;
- descrição textual;
- um PDF opcional.

O PDF aceita até 10 MB e 50 páginas. Validações de formato, tamanho e páginas devem acontecer antes da reserva de cota.

A resposta confirma o recebimento e identifica o relatório em `PENDING`; ela não espera a execução do Ollama.

Telegram e API iniciam o mesmo caso de uso de aplicação e obedecem às mesmas regras de validação, cota e processamento.

## Consulta

A API permitirá consultar, de acordo com autorização:

- conta atual;
- vínculo com Telegram;
- relatório individual;
- histórico paginado de relatórios;
- estado do processamento;
- cota semanal atual;
- histórico de consumo;
- métricas e custo estimado;
- operações administrativas já definidas.

A lista definitiva de rotas ainda será decidida antes da specification de implementação.

## Resultado e arquivos

O conteúdo completo continuará no MinIO. O cliente fará download por endpoint autenticado da API, que validará conta e autorização antes de transmitir o arquivo.

Não haverá URL pública permanente ou armazenamento do Markdown completo no PostgreSQL.

## Autorização

- usuários acessam somente os próprios recursos;
- endpoints administrativos exigem a role `ADMIN`;
- o identificador recebido na URL nunca substitui a verificação de propriedade;
- o bot usa o vínculo Telegram confirmado para localizar a conta, mas executa as mesmas regras do caso de uso.

## Erros e rastreabilidade

Cada requisição terá um correlation ID. O identificador será propagado nos logs e poderá ser retornado em respostas de erro para facilitar diagnóstico, sem expor detalhes internos sensíveis.

Erros de validação, autenticação, autorização, cota esgotada, recurso inexistente e conflito devem ser representados de forma consistente por Problem Details. O catálogo definitivo de códigos ainda será definido.

## OpenAPI

O OpenAPI será o contrato técnico de integração com o futuro frontend. Ele documentará campos, validações, autenticação, estados e erros, mas não decisões visuais ou tecnologia da interface.
