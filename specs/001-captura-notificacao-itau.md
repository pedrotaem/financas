# Spec 001 — Captura de notificações do Itaú

**Status:** Implementada e validada em produção (20/07/2026) — PIX enviado e recebido testados com transações reais no S24 Ultra; parser acertou ambos. **Pendente:** calibrar regex de compra no cartão (sem texto real ainda; fallback de captura bruta cobre até lá)
**Contexto:** Captura
**Depende de:** Spec 000

## História

> Como usuário, quando recebo notificação do app Itaú (compra no cartão de crédito, PIX enviado ou recebido), quero que o app registre o lançamento automaticamente, para eu não digitar nada.

## Mecanismo

`NotificationListenerService` (API nativa Android). Requer permissão especial concedida pelo usuário em *Configurações → Notificações → Acesso a notificações* (o app abre essa tela na primeira execução).

- Filtrar por pacote: `com.itau` (verificar pacote real no aparelho — pode variar; tornar configurável)
- Escutar apenas notificações novas (`onNotificationPosted`); não remover a notificação original
- Serviço deve sobreviver a reinicialização (re-bind automático do sistema)

## Parsing

Parser por regex sobre `title` + `text` + `bigText` da notificação. Padrões esperados (validar com notificações reais do aparelho — **primeira tarefa da implementação é logar o texto cru para calibrar os regex**):

| Evento | Padrão aproximado | Resultado |
|---|---|---|
| Compra crédito | "Compra aprovada … R$ 54,90 … MERCADO X" | DEBITO, valor, estabelecimento |
| PIX enviado | "Você pagou R$ 100,00 para Fulano" | DEBITO, valor, descrição |
| PIX recebido | "Você recebeu um Pix de R$ 250,00 de Fulano" | CREDITO, valor, descrição |

### Regras

1. Parse OK → cria `Lancamento` com `origem = NOTIFICACAO_ITAU`, `status = PENDENTE_REVISAO`
2. Parse falhou mas é notificação do Itaú com valor monetário detectado → registra **captura bruta** (texto completo) para revisão manual; nada é perdido silenciosamente
3. **Dedup:** hash(pacote + texto + janela de 2 min) — notificações repetidas/atualizadas não duplicam lançamento
4. Texto cru da notificação é armazenado junto ao lançamento (auditoria + re-parse futuro)

## Critérios de aceite

- [ ] Compra de cartão de crédito no Itaú gera lançamento DEBITO com valor e estabelecimento corretos
- [ ] PIX enviado gera DEBITO; PIX recebido gera CREDITO
- [ ] Notificação não reconhecida do Itaú aparece como "captura pendente" para classificação manual
- [ ] Mesma notificação processada 2× → 1 lançamento
- [ ] App reiniciado/celular reiniciado → serviço volta a capturar sem intervenção
- [ ] Notificações de outros apps são ignoradas

## Testes

- Unit: parser com fixtures de textos reais de notificação (arquivo `fixtures/notificacoes-itau.txt`)
- Unit: dedup
- Manual no aparelho: roteiro com PIX de R$ 0,01

## Riscos

| Risco | Mitigação |
|---|---|
| Itaú muda formato do texto | Captura bruta como fallback; parser plugável; fixtures versionadas |
| Sistema mata o serviço (economia de bateria Samsung) | Instruir usuário a remover app da otimização de bateria |
| Pacote do Itaú diferente do esperado | Tela de debug lista pacotes das notificações recebidas |
