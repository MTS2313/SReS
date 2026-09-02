# Planos de relatórios

## Estado

**Planejado.**

## Objetivo

Representar o produto de cotas semanais sem introduzir cobrança, assinatura ou faturamento no MVP.

## Conceito

Um plano de relatórios define:

- nome;
- quantidade de relatórios permitidos por semana;
- estado ativo ou inativo;
- indicação de plano padrão, quando aplicável.

A primeira configuração será o **Plano Inicial**, com 10 relatórios por semana, e será o plano padrão.

## Relação com contas

- cada conta possui um plano de relatórios;
- novas contas recebem automaticamente o plano padrão;
- um administrador pode atribuir outro plano a uma conta;
- usuários não escolhem ou contratam planos pelo sistema nesta versão;
- uma conta continua podendo receber ajustes administrativos de cota além do valor do plano.

## Troca de plano

A troca realizada pelo administrador entra em vigor imediatamente.

Ao trocar:

1. o novo plano é associado à conta;
2. a cota disponível é reiniciada com o limite completo do novo plano;
3. o novo saldo permanece válido até a próxima renovação semanal regular;
4. o consumo anterior continua no histórico, mas não reduz a nova cota;
5. relatórios já reservados ou em processamento permanecem registrados no período anterior;
6. plano anterior, novo plano, administrador, motivo e data são auditados.

A troca pode conceder uma nova cota completa dentro da mesma semana. Esse é um efeito intencional da operação administrativa.

## Administração

A role `ADMIN` poderá:

- consultar planos;
- criar ou alterar planos;
- ativar ou inativar planos;
- definir o plano padrão;
- atribuir um plano a uma conta.

Inativar um plano impede novas atribuições, mas não remove as contas já associadas. Essas contas continuam usando o plano até que um administrador realize a migração.

Histórico de planos e atribuições não será apagado.

## Limites

Não fazem parte do MVP:

- preços comerciais;
- checkout;
- pagamento;
- renovação financeira;
- faturas;
- assinatura contratual;
- compra avulsa de relatórios.

O plano define capacidade de uso, não cobrança.
