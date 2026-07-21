# Finanças — app pessoal de acompanhamento financeiro

App Android (Kotlin) open source para registro automático de gastos:

- **Notificações do Itaú** → lançamentos automáticos (cartão de crédito, PIX)
- **Foto de nota fiscal** → extração de itens via Claude API

Dados 100% locais no aparelho (Room/SQLite). Sem backend.

## Desenvolvimento

Projeto segue **spec-driven design** + **DDD**. Toda feature nasce como spec em [`specs/`](specs/) antes do código.

| Spec | Feature |
|---|---|
| [000](specs/000-visao-geral.md) | Visão geral, modelo de domínio, arquitetura |
| [001](specs/001-captura-notificacao-itau.md) | Captura de notificações do Itaú |
| [002](specs/002-leitura-nota-fiscal.md) | Leitura de nota fiscal por foto |

## Requisitos

- Android Studio (ou JDK 17 + Android SDK)
- Aparelho Android 14+ (desenvolvido no Samsung S24 Ultra)
- API key da Anthropic (para a feature de nota fiscal) — configurada no app, nunca commitada

## Licença

MIT (a definir)
