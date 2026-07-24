# Spec 006 — Orçamento por categoria (real × planejado)

**Status:** Implementada (24/07/2026) — testes unit ok, APK instalado; aguardando validação manual no aparelho (incl. migração 1→2 sobre o banco real)
**Contexto:** Gestão Financeira + UI
**Depende de:** Spec 003, 005

## História

> Como usuário, quero definir quanto pretendo gastar por categoria no mês e acompanhar o consumo desse orçamento — real × planejado — para saber onde estou estourando antes do mês acabar.

## Decisões

| Decisão | Escolha |
|---|---|
| Modelo | Orçamento **mensal recorrente** por categoria: um valor por `Categoria`, vale para todo mês. Sem orçamento por mês específico (fora de escopo) |
| Consumo | Só `DEBITO` conta (consistente com o donut, spec 003 regra 3); mês = mês selecionado no dashboard |
| Sem categoria | Não orçável — orçamento exige `Categoria` concreta |
| Persistência | Nova tabela Room `orcamentos` (categoria PK, valorCentavos); migração 1→2 preservando dados |
| Acesso | Card **Orçamento** sempre visível no dashboard (entre saldo e donut): resumo total gasto × planejado + até 3 categorias mais consumidas; toque → tela Orçamento (rota própria, fora da bottom nav) |
| Tela Orçamento | Lista todas as categorias com planejado, gasto do mês e barra de progresso; toque na linha → diálogo para definir valor (ou **Remover** se já definido) |
| Total do card | Soma dos planejados × soma dos gastos **das categorias orçadas** (gasto fora de orçamento não polui o total) |
| Cor da barra | ≤80% `primary` · 80–100% `tertiary` · >100% (estourado) `error` |
| Privacidade | Olho da spec 005 mascara valores do orçamento também; barras de progresso continuam visíveis (proporção não revela valor) |
| Zerar | Definir valor vazio ou R$ 0 remove o orçamento da categoria |

## Regras

1. Progresso = gasto do mês na categoria ÷ planejado; categoria orçada sem gasto no mês mostra 0%.
2. Gasto em categoria **sem** orçamento não aparece no card do dashboard (aparece na tela Orçamento como linha sem meta, para convidar a orçar).
3. Estouro (>100%) não é erro: barra cheia em `error` + percentual real (ex.: 143%).
4. Card do dashboard sem nenhum orçamento definido mostra estado vazio acolhedor com chamada para configurar.
5. Cálculo em domain puro (`ProgressoOrcamento`), testável sem Android.

## Critérios de aceite

- [ ] Definir R$ X para uma categoria e ver a barra refletir os gastos do mês imediatamente
- [ ] Trocar o mês no dashboard recalcula o consumo (orçamento é o mesmo, gasto muda)
- [ ] Estourar o orçamento pinta a barra de `error` com percentual >100%
- [ ] Remover orçamento tira a categoria do card do dashboard
- [ ] Valores mascarados quando o saldo está oculto; barras continuam visíveis
- [ ] Migração 1→2 preserva lançamentos e capturas existentes no aparelho

## Testes

- Unit: cálculo de progresso (sem gasto, parcial, estourado, categoria não orçada ignorada no total)
- Manual no aparelho: fluxo completo + migração sobre o banco real
