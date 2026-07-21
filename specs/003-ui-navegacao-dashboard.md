# Spec 003 — Navegação e Dashboard

**Status:** Aprovada (decisões do usuário em 20/07/2026)
**Contexto:** UI
**Depende de:** Spec 000, 001

## História

> Como usuário, quero abrir o app e ver minha situação financeira do mês num relance — saldo, para onde o dinheiro foi, e o que precisa de revisão — como nos apps famosos de finanças (Mobills, Organizze).

## Decisões

| Decisão | Escolha |
|---|---|
| Navegação | Bottom nav 3 abas: **Início** · **Lançamentos** · **Ajustes** + FAB |
| Dashboard | Card saldo do mês · Donut despesas por categoria · Alerta de pendências |
| Gráficos | Canvas Compose desenhado à mão (zero dependência) |
| Tema | Material 3 dynamic color (já existente) |

## Telas

### Início (dashboard)
- Seletor de mês: `← Julho 2026 →` (navega histórico)
- **Card saldo:** receitas − despesas do mês; linhas ↑ receitas (verde) e ↓ despesas (vermelho)
- **Alerta pendências:** card destacado se houver lançamentos `PENDENTE_REVISAO` ou capturas brutas; toque → aba Lançamentos
- **Donut por categoria:** despesas do mês fatiadas por `Categoria`; centro mostra total; legenda com valor por fatia. Sem categoria → fatia "Sem categoria"
- Últimos 5 lançamentos do mês

### Lançamentos
- Lista completa (mês selecionado herdado do dashboard), capturas brutas no topo
- Ações por item: **Confirmar** (status → CONFIRMADO), **Categorizar** (dropdown de `Categoria`)

### Ajustes
- Status/botão de acesso a notificações (movido do card da home)
- (Spec 002 adicionará: campo de API key)

### FAB (+)
- Diálogo de lançamento manual: tipo (débito/crédito), valor, descrição, categoria → salva com `origem = MANUAL`, `status = CONFIRMADO`
- (Spec 002 adicionará: opção "fotografar nota")

## Regras

1. Totais do mês incluem lançamentos `PENDENTE_REVISAO` (visão realista; pendência é sobre revisão, não sobre existência)
2. Mês = `dataHora` do lançamento no fuso do aparelho
3. Donut considera apenas `DEBITO`
4. Cores de categoria: paleta fixa (mesma cor sempre p/ mesma categoria)

## Critérios de aceite

- [ ] Abrir app → dashboard do mês corrente com saldo correto
- [ ] Navegar para mês anterior mostra dados daquele mês
- [ ] Donut reflete despesas por categoria; lançamento sem categoria aparece como "Sem categoria"
- [ ] Card de pendências some quando não há nada a revisar
- [ ] Lançamento manual criado pelo FAB aparece no dashboard e na lista
- [ ] Categorizar lançamento atualiza donut imediatamente

## Testes

- Unit: agregação por mês e por categoria (receitas, despesas, fatias)
- Manual no aparelho: fluxo completo com dados reais capturados
