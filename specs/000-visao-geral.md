# Spec 000 — Visão Geral e Modelo de Domínio

**Status:** Rascunho — aguardando aprovação
**App:** Planejamento e acompanhamento financeiro pessoal (open source)
**Plataforma:** Android nativo (Kotlin), alvo: Samsung S24 Ultra (Android 14+)

## Objetivo

Registrar gastos e receitas automaticamente a partir de duas fontes:
1. Notificações do app Itaú (compra no cartão de crédito, PIX enviado/recebido)
2. Foto de nota fiscal (extração automática dos itens via LLM)

Dados 100% locais no aparelho (SQLite/Room). Sem backend. Export CSV/JSON para backup.

## Decisões de arquitetura

| Decisão | Escolha | Motivo |
|---|---|---|
| Stack | Kotlin + Jetpack Compose | `NotificationListenerService` é API nativa; melhor fit p/ MVP |
| Persistência | Room (SQLite) local | Privacidade, zero infra |
| Extração de nota | Foto → Claude API (vision + structured output) | Extração robusta sem parser de OCR |
| Modelo LLM | `claude-opus-4-8` | Vision + structured outputs; ~US$0,01–0,03 por foto |
| SDK LLM | `com.anthropic:anthropic-java` (Kotlin usa SDK Java) | SDK oficial |
| Arquitetura | DDD em camadas + spec-driven | Specs em `specs/` antes de cada feature |

## Linguagem ubíqua

| Termo | Significado |
|---|---|
| **Lançamento** | Registro financeiro (entrada ou saída). Agregado raiz do domínio. |
| **Origem** | De onde veio o lançamento: `NOTIFICACAO_ITAU`, `NOTA_FISCAL`, `MANUAL` |
| **Captura** | Processo de transformar evento externo (notificação, foto) em Lançamento |
| **Nota Fiscal** | Cupom/NFC-e fotografado; contém itens que detalham um Lançamento |
| **Categoria** | Classificação do gasto (Mercado, Transporte, Lazer...) |
| **Revisão** | Lançamento capturado nasce `PENDENTE_REVISAO`; usuário confirma → `CONFIRMADO` |

## Bounded contexts

```
┌─────────────────────┐      ┌──────────────────────┐
│  Captura            │      │  Gestão Financeira    │
│  - Parser notif.    │─────▶│  - Lançamento (raiz)  │
│  - Extração nota    │ cria │  - Categoria          │
│    (Claude API)     │      │  - Orçamento (futuro) │
└─────────────────────┘      └──────────────────────┘
```

## Modelo de domínio (núcleo)

```kotlin
// domain/ — Kotlin puro, sem Android
Lancamento(
    id: LancamentoId,
    tipo: Tipo,            // DEBITO | CREDITO
    valor: Dinheiro,       // BigDecimal centavos + moeda BRL
    dataHora: Instant,
    descricao: String,
    estabelecimento: String?,
    categoria: Categoria?,
    origem: Origem,        // NOTIFICACAO_ITAU | NOTA_FISCAL | MANUAL
    status: Status,        // PENDENTE_REVISAO | CONFIRMADO
    notaFiscalId: NotaFiscalId?,
)

NotaFiscal(
    id: NotaFiscalId,
    estabelecimento: String,
    cnpj: String?,
    dataEmissao: LocalDate?,
    total: Dinheiro,
    itens: List<ItemNota>, // descricao, quantidade, valorUnitario, valorTotal
)
```

## Camadas (DDD)

```
app/src/main/java/dev/pedro/financas/
├── domain/          # Entidades, VOs, regras. Kotlin puro. Zero dependência Android.
├── application/     # Casos de uso: RegistrarLancamento, CapturarNotificacao, ExtrairNotaFiscal
├── infrastructure/  # Room, NotificationListenerService, cliente Claude, DataStore
└── ui/              # Compose: lista de lançamentos, revisão, câmera, config
```

Regra de dependência: `ui → application → domain` ← `infrastructure`. Domain não conhece ninguém.

## Fora de escopo do MVP

- Backend / sync em nuvem
- Multi-usuário / auth
- Orçamentos e metas (fase 2)
- Outros bancos além do Itaú (fase 2 — parser é plugável)
- Leitura de QR code da NFC-e / consulta SEFAZ (fase 2 — melhoraria precisão)

## Specs de feature

- [001 — Captura de notificações do Itaú](001-captura-notificacao-itau.md)
- [002 — Leitura de nota fiscal por foto](002-leitura-nota-fiscal.md)
