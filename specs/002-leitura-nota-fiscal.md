# Spec 002 — Leitura de nota fiscal por foto

**Status:** Rascunho — aguardando aprovação
**Contexto:** Captura
**Depende de:** Spec 000

## História

> Como usuário, quando fotografo uma nota fiscal, quero que o app extraia estabelecimento, total e itens automaticamente, para registrar o gasto detalhado sem digitação.

## Fluxo

```
Câmera (CameraX) → foto → redimensionar (máx ~1568px lado maior, JPEG q80)
  → base64 → Claude API (vision + structured output)
  → JSON validado → tela de revisão → usuário confirma/edita → salva
      → Lancamento (DEBITO, origem = NOTA_FISCAL) + NotaFiscal com itens
```

## Chamada à API

- SDK: `com.anthropic:anthropic-java`
- Modelo: `claude-opus-4-8`
- Imagem: bloco `image` base64 + instrução de extração
- Structured output: `output_config.format` com `json_schema` — resposta sempre JSON válido, sem parse frágil

### Schema de saída

```json
{
  "type": "object",
  "properties": {
    "estabelecimento": {"type": "string"},
    "cnpj": {"type": ["string", "null"]},
    "data_emissao": {"type": ["string", "null"], "description": "ISO 8601 (YYYY-MM-DD)"},
    "total_centavos": {"type": "integer"},
    "itens": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "descricao": {"type": "string"},
          "quantidade": {"type": "number"},
          "valor_unitario_centavos": {"type": "integer"},
          "valor_total_centavos": {"type": "integer"}
        },
        "required": ["descricao", "quantidade", "valor_unitario_centavos", "valor_total_centavos"],
        "additionalProperties": false
      }
    },
    "categoria_sugerida": {
      "type": "string",
      "enum": ["MERCADO", "RESTAURANTE", "FARMACIA", "TRANSPORTE", "COMBUSTIVEL", "LAZER", "VESTUARIO", "CASA", "OUTROS"]
    },
    "legivel": {"type": "boolean", "description": "false se a foto não permite extração confiável"}
  },
  "required": ["estabelecimento", "total_centavos", "itens", "categoria_sugerida", "legivel"],
  "additionalProperties": false
}
```

Valores em **centavos (integer)** — nunca float para dinheiro.

### Regras

1. `legivel = false` → app pede nova foto (não salva nada)
2. Soma dos itens ≠ total → aviso na tela de revisão (não bloqueia; notas têm descontos/arredondamentos)
3. Sem internet → foto fica em fila local, processa quando conectar
4. Erro de API (429/5xx) → retry automático do SDK; falha final → fila
5. Foto original armazenada localmente vinculada à nota (auditoria); opção de apagar após confirmação

## API key

- Usuário informa a própria key do Claude (`ANTHROPIC_API_KEY`) na tela de configurações
- Armazenada com `EncryptedSharedPreferences`/`DataStore` criptografado — nunca em código, nunca em log
- App é open source: key jamais commitada

## Custo estimado

Foto ~1600–4800 tokens entrada + ~500 saída em `claude-opus-4-8` (US$5/US$25 por MTok) → **~US$0,01–0,03 por nota** (~R$0,05–0,15).

## Critérios de aceite

- [ ] Foto de cupom fiscal legível → estabelecimento, total e itens corretos na tela de revisão
- [ ] Usuário edita qualquer campo antes de salvar
- [ ] Confirmar → cria `Lancamento` + `NotaFiscal` vinculados
- [ ] Foto ilegível → mensagem pedindo nova foto
- [ ] Sem internet → entra na fila; processa ao reconectar
- [ ] API key nunca aparece em log/commit

## Testes

- Unit: mapeamento JSON → domínio; validação soma itens vs total
- Integração: chamada real com 3 fotos-fixture (nota legível, amassada, não-nota) — roda manualmente, não no CI
- Manual: fotografar nota real no aparelho

## Riscos

| Risco | Mitigação |
|---|---|
| Custo por foto | Uso pessoal (~30 notas/mês ≈ US$1); modelo configurável no futuro |
| Alucinação de valores | Tela de revisão obrigatória antes de salvar; aviso de divergência de soma |
| Foto ruim | Campo `legivel` no schema; orientação de enquadramento na UI |
