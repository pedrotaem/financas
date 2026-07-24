# Spec 007 — Captura de SMS de contas (Claro/Vivo) e lançamento futuro

**Status:** Implementada (24/07/2026) — testes unit ok, APK instalado; pendente: conceder permissão de SMS em Ajustes e validar com SMS reais na próxima fatura (logcat tag `CapturaSms`)
**Contexto:** Captura + Gestão Financeira
**Depende de:** Spec 001 (dedup, captura bruta), 004, 006

## História

> Como usuário, quero que os SMS de pagamento das minhas contas Claro e Vivo virem lançamentos automaticamente — e que a conta da Vivo, avisada dias antes do débito, apareça como lançamento **futuro** que só entra nos números quando o pagamento é confirmado.

## Mensagens (formatos reais)

| Operadora | Momento | Exemplo |
|---|---|---|
| Claro | Pagamento confirmado (única) | "Cliente Claro, o pagamento automatico da sua fatura de julho no valor de R$ 199,99 foi confirmado! ..." (valor fictício — repo público) |
| Vivo | Aviso ~4 dias antes | "Ola, sua conta Vivo em debito automatico no valor de R$149,90 vence em 17/07. ..." (valor fictício) |
| Vivo | Confirmação no dia | "Ola, deu tudo certo com o pagamento da fatura Vivo em debito automatico de vencimento em 17/07. ..." |

Atenção: valor da Vivo vem **sem espaço** após "R$"; textos chegam sem acento.

## Decisões

| Decisão | Escolha |
|---|---|
| Canal | `BroadcastReceiver` de `SMS_RECEIVED` (permissão `RECEIVE_SMS`, runtime, concedida em Ajustes). Corpo completo e multipart concatenado — sem truncamento de notificação. App pessoal sideloaded: sem restrição de Play Store |
| Lançamento futuro | Novo `Status.FUTURO`: existe, é visível, mas **fora** de saldo, donut, orçamento e contagem de pendências |
| Origem | Um único valor `Origem.NOTIFICACAO_SMS` para todo SMS capturado; a operadora fica na descrição e no `textoOrigem` |
| Claro | Mensagem única já confirmada → lançamento `DEBITO` `PENDENTE_REVISAO`, descrição "Fatura Claro (julho)" |
| Vivo aviso | Cria lançamento `FUTURO`, `dataHora` = vencimento (12h, fuso local), descrição "Fatura Vivo". Aviso repetido para o mesmo vencimento atualiza o valor (não duplica) |
| Vivo confirmação | Não traz valor — **chave de casamento = vencimento + origem SMS + descrição "Fatura Vivo"**. Encontra o futuro e o efetiva (`FUTURO → PENDENTE_REVISAO`, dataHora mantida = vencimento). Sem futuro correspondente → captura bruta (fila de não reconhecidos) |
| Ano do vencimento | "17/07" sem ano: ano corrente; se a data resultante ficar >30 dias no passado, ano seguinte (virada dez→jan) |
| SMS irrelevante | Ignorado — SMS é fonte restrita: só padrões conhecidos geram registro; spam com valor não entra. (Única captura bruta vinda de SMS: confirmação Vivo órfã) |
| Dedup | Mesmo registro hash+janela 2min da spec 001 (multipart/reentrega) |
| UI | Seção **"Futuros"** na aba Lançamentos (todas as datas, ordenada por vencimento); cartão de futuro tem **Efetivar** (→ PENDENTE_REVISAO, para quando a confirmação não chegar) e **Rejeitar**. Ajustes ganha card de permissão de SMS |
| Categoria | Nenhuma automática — usuário categoriza na revisão |

## Regras

1. `FUTURO` fora de `ResumoMensal` (saldo, receitas/despesas, fatias) — donut e orçamento herdam a exclusão.
2. `FUTURO` fora de `qtdPendentes` (não é pendência de revisão; ainda não aconteceu).
3. Efetivação preserva id, valor, `dataHora` (vencimento) e `textoOrigem` do aviso.
4. Auditoria: `textoOrigem` guarda o corpo completo do SMS.

## Critérios de aceite

- [ ] SMS Claro cria lançamento pendente com o valor da fatura e descrição "Fatura Claro (julho)"
- [ ] SMS aviso Vivo cria futuro com o valor e a data de vencimento; saldo/donut/orçamento não mudam
- [ ] SMS confirmação Vivo converte o futuro em pendente; totais passam a contar
- [ ] Confirmação sem futuro correspondente cai na fila de capturas não reconhecidas
- [ ] "Efetivar" manual funciona quando a confirmação não chega
- [ ] Permissão de SMS solicitável em Ajustes com status visível

## Testes

- Unit: parser (3 formatos, R$ sem espaço, inferência de ano dez→jan, spam ignorado), caso de uso (futuro criado/atualizado/efetivado, fallback captura bruta, dedup), `ResumoMensal` exclui futuro
- Manual: SMS reais na próxima fatura; logcat tag `CapturaSms`
