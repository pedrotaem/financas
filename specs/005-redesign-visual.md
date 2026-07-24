# Spec 005 — Redesign visual (design system)

**Status:** Implementada (24/07/2026) — testes unit ok, APK instalado; aguardando validação manual no aparelho. Nota: minSdk 31 ⇒ o fallback estático verde-esmeralda nunca aparece na prática (dynamic color sempre disponível)
**Contexto:** UI
**Depende de:** Spec 003, 004
**Base:** Pesquisa 23/07/2026 sobre design systems de fintechs (Nubank dark mode/tokens, redesign Home do Monzo 2023, guias Material 3). Fontes principais: building.nubank.com (dark mode), monzo.com/blog (home screen), m3.material.io (color roles), developer.android.com (Compose M3).

## História

> Como usuário, quero abrir o app e ter a sensação dos bons apps de banco — o número que importa em destaque, cartões limpos em camadas, cores coerentes no claro e no escuro — e poder esconder meus saldos quando alguém está olhando a tela.

## Princípios (extraídos da pesquisa)

1. **Resumo primeiro, detalhe no toque** (Monzo): o dashboard é uma visão única e resumida; cada card é tocável e leva ao detalhe. Números-chave nunca escondidos por padrão.
2. **Tokens, nunca cor hard-coded** (Nubank): toda cor vem de papel semântico do `ColorScheme`; o mesmo componente rende em claro/escuro sem `if`.
3. **Hierarquia por tom, não por sombra** (M3): fundo `surface`, cards em `surfaceContainer*`, elevação zero.
4. **Contraste é regra, não sorte** (M3): pares `x`/`onX` sempre juntos; texto monetário pequeno usa `onSurface`/`onSurfaceVariant` (4.5:1), cores de acento só em texto grande (3:1).

## Decisões

| Decisão | Escolha |
|---|---|
| Âncora de cor | Dynamic color mantido (Android 12+). Fallback estático ganha semente própria **verde-esmeralda `#006C4C`** (hoje é o baseline M3 sem identidade) via `lightColorScheme`/`darkColorScheme` derivados da semente |
| Camadas | Fundo `surface` · cards `surfaceContainer` · card de destaque (saldo) `surfaceContainerHigh` · sem `elevation` |
| Forma | Cantos 20dp nos cards (`RoundedCornerShape(20.dp)` via `MaterialTheme.shapes.large`) |
| Tipografia | Papéis M3 aplicados por função — ver tabela abaixo |
| Privacidade de saldo | Ícone de olho no card de saldo alterna `R$ ••••` em **todos** os valores do app (saldo, receitas, despesas, lançamentos, donut). Persistido em `SharedPreferences` (zero dependência nova) |
| Dark mode OLED | Toggle "Preto OLED" em Ajustes: no escuro, `surface`/`background` → preto puro `#000000`, containers rebaixados em conjunto (à la Nubank). Persistido junto |
| Cores de categoria | Mantém matiz fixo por categoria (spec 003, regra 4 — reconhecimento), mas em **par tonal claro/escuro** e harmonizado com o tema: `lerp(corCategoria, primary, 0.15f)` aproxima as fatias da paleta do aparelho sem perder distinção |
| Donut | `StrokeCap.Round` nas pontas, animação de varredura na entrada (`animateFloatAsState`), fatia tocável destaca a linha correspondente na legenda |
| Estados vazios | Ícone + frase acolhedora + ação ("Nenhum lançamento neste mês. Toque em + para começar"), em vez de texto seco |
| Microinterações | Transições de valor com `AnimatedContent` no saldo ao trocar de mês; nada além disso (sobriedade > espetáculo) |

## Tabela tipográfica

| Elemento | Papel M3 |
|---|---|
| Saldo do mês (herói do dashboard) | `displaySmall` |
| Total no centro do donut | `titleLarge` |
| Seletor de mês | `titleMedium` |
| Cabeçalho de card / seção | `titleMedium` |
| Valor em linha de lançamento | `bodyLarge` |
| Descrição de lançamento | `bodyLarge` |
| Data, categoria, metadados | `labelMedium` em `onSurfaceVariant` |
| Legenda do donut | `bodyMedium` |

## Estrutura do dashboard (resumo → detalhe)

1. Seletor de mês (mantido no topo)
2. **Card saldo (herói):** `surfaceContainerHigh`, saldo em `displaySmall`, olho de privacidade no canto; receitas/despesas como linhas secundárias em `onSurfaceVariant`; toque → aba Lançamentos
3. **Card pendências:** mantido (`tertiaryContainer`), some quando zerado
4. **Card categoria:** donut compacto + top 3 categorias na legenda; toque → detalhe completo (legenda inteira). Primeira entrega pode manter legenda completa
5. Últimos 5 lançamentos (mantido)

## Regras

1. Nenhum `Color(0x...)` fora do arquivo de tema e da paleta de categorias; componentes só usam `MaterialTheme.colorScheme.*`.
2. Valor monetário em texto pequeno nunca usa `primary`/`error` como cor — sinal (+/−) e ícone carregam a semântica; exceção: valores em `bodyLarge` ou maior podem manter cor.
3. Ocultar saldo cobre todo valor monetário renderizado, inclusive centro do donut e legenda.
4. Preferências (`saldoOculto`, `temaOled`) sobrevivem a processo morto e reinstalação de tela (SharedPreferences, leitura no `AppContainer`, expostas como `StateFlow` no ViewModel).
5. Tudo continua funcionando em Android < 12 com o esquema estático da semente `#006C4C`.

## Critérios de aceite

- [ ] Dashboard com cards em camadas tonais, sem sombras, cantos 20dp
- [ ] Saldo do mês em destaque tipográfico claro (displaySmall), legível no claro e no escuro
- [ ] Olho oculta/revela todos os valores; estado persiste após matar o app
- [ ] Toggle "Preto OLED" em Ajustes muda o fundo escuro para preto puro imediatamente
- [ ] Fatias do donut mantêm a mesma família de cor por categoria, mas harmonizadas com o tema do aparelho
- [ ] Donut anima a varredura ao entrar na tela
- [ ] Em aparelho Android < 12 (ou emulador API 30), o app usa a paleta verde-esmeralda
- [ ] Estados vazios com ícone e chamada para ação

## Fora de escopo

- Layout customizável pelo usuário (Monzo "Edit layout") — avaliar em spec futura se o dashboard crescer
- Troca de fonte (Gellix etc.) — fica a tipografia padrão do sistema
- Biometria para revelar saldo

## Testes

- Unit: formatação mascarada (`R$ ••••`), par tonal de categoria (claro/escuro distintos, determinísticos)
- Manual no aparelho: claro/escuro, OLED on/off, dynamic color com wallpapers diferentes, ocultar saldo e matar o app
