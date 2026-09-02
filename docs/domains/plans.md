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

A primeira configuração terá uma cota de 10 relatórios por semana. O nome comercial do plano ainda não foi decidido.

## Relação com contas

- cada conta possui um plano de relatórios;
- novas contas recebem automaticamente o plano padrão;
- um administrador pode atribuir outro plano a uma conta;
- usuários não escolhem ou contratam planos pelo sistema nesta versão;
- uma conta continua podendo receber ajustes administrativos de cota além do valor do plano.

A regra de vigência quando um plano é trocado durante um período semanal ainda precisa ser definida.

## Administração

A role `ADMIN` poderá:

- consultar planos;
- criar ou alterar planos;
- ativar ou inativar planos;
- definir o plano padrão;
- atribuir um plano a uma conta.

Inativar um plano impede novas atribuições, mas não deve apagar seu histórico nem alterar silenciosamente contas já associadas. O comportamento de contas ainda vinculadas a um plano inativado será definido antes da implementação.

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
