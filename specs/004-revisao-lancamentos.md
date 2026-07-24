# Spec 004 — Revisão de lançamentos e capturas

**Status:** Implementada (23/07/2026) — testes unit ok, APK instalado; aguardando validação manual no aparelho
**Contexto:** UI + Gestão Financeira
**Depende de:** Spec 001, 003

## História

> Como usuário, quero corrigir o que o app registrou — editar um lançamento errado, rejeitar uma captura que não é minha despesa, e resolver as notificações que o parser não entendeu — para que a lista reflita a realidade sem eu precisar aceitar tudo cegamente.

## Decisões

| Decisão | Escolha |
|---|---|
| Editar | Toque no cartão do lançamento → mesmo diálogo do FAB, pré-preenchido. Editáveis: tipo, valor, descrição, categoria. Preservados: id, dataHora, origem, status, textoOrigem, notaFiscalId. O diálogo de edição também oferece **Excluir** (qualquer lançamento, não só pendente) |
| Rejeitar | Botão **Rejeitar** ao lado de **Confirmar** (só em `PENDENTE_REVISAO`) → exclui o lançamento definitivamente |
| Captura não reconhecida | Dois botões no cartão: **Descartar** (marca `processada = true`, some da lista) e **Adicionar** (diálogo pré-preenchido com valor detectado → cria lançamento) |
| Lançamento vindo de captura | `origem = NOTIFICACAO_ITAU`, `status = CONFIRMADO` (usuário acabou de revisar), `dataHora` da captura (momento real da notificação), `textoOrigem` = texto da captura (auditoria, spec 001) |
| Diálogo | `AdicionarLancamentoDialog` generalizado em `LancamentoDialog` (título + valores iniciais); FAB continua abrindo em branco |

## Regras

1. Rejeitar exclui de vez — não há lixeira. O registro de dedup (spec 001, regra 3) impede que a mesma notificação recrie o lançamento.
2. Editar não muda o status: revisão continua sendo um ato explícito (botão Confirmar).
3. Adicionar a partir de captura marca a captura como `processada` na mesma operação — ela nunca gera dois lançamentos.
4. Descartar não apaga a captura do banco (auditoria preservada), só a tira da fila de pendências.

## Critérios de aceite

- [ ] Tocar num lançamento abre diálogo com os valores atuais; salvar atualiza a lista e o dashboard
- [ ] Editar lançamento pendente não o confirma
- [ ] Rejeitar lançamento pendente o remove da lista e dos totais do mês
- [ ] Captura não reconhecida some ao Descartar
- [ ] Adicionar a partir de captura cria lançamento com o valor detectado pré-preenchido e remove a captura da fila
- [ ] Contador de pendências do dashboard reflete rejeições e descartes imediatamente

## Testes

- Unit: `CapturaBruta.processar`, preservação de campos na edição (domain puro)
- Manual no aparelho: fluxo completo com capturas reais
