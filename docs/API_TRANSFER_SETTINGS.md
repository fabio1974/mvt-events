# API de Configuração de Saque Automático

## Endpoint

```
POST /api/bank-accounts
PUT  /api/bank-accounts
```

## Payload Completo

```json
{
  "user": {
    "id": "uuid-do-usuario"
  },
  "bankCode": "001",
  "bankName": "Banco do Brasil",
  "agency": "1234",
  "agencyDigit": "5",
  "accountNumber": "123456",
  "accountDigit": "7",
  "accountType": "CHECKING",
  
  "motherName": "Maria Silva",
  "monthlyIncome": "5000",
  "professionalOccupation": "Entregador",
  
  "transferInterval": "Daily",
  "transferDay": 0
}
```

---

## Campos de Transferência

### `transferInterval` (String)

Define a frequência do saque automático.

| Valor | Descrição |
|-------|-----------|
| `"Daily"` | Saque todo dia útil |
| `"Weekly"` | Saque em um dia específico da semana |
| `"Monthly"` | Saque em um dia específico do mês |

**Default:** `"Daily"`

---

### `transferDay` (Integer)

Define o dia do saque, conforme o intervalo escolhido.

#### Para `"Daily"`:
| Valor | Descrição |
|-------|-----------|
| `0` | Todo dia útil (único valor válido) |

#### Para `"Weekly"`:
| Valor | Dia da Semana |
|-------|---------------|
| `0` | Domingo |
| `1` | Segunda-feira |
| `2` | Terça-feira |
| `3` | Quarta-feira |
| `4` | Quinta-feira |
| `5` | Sexta-feira |
| `6` | Sábado |

#### Para `"Monthly"`:
| Valor | Descrição |
|-------|-----------|
| `1` a `31` | Dia do mês |

**Default:** `0`

---

## Exemplos de Uso

### Saque Diário (todo dia útil)
```json
{
  "transferInterval": "Daily",
  "transferDay": 0
}
```

### Saque Semanal (toda sexta-feira)
```json
{
  "transferInterval": "Weekly",
  "transferDay": 5
}
```

### Saque Semanal (toda segunda-feira)
```json
{
  "transferInterval": "Weekly",
  "transferDay": 1
}
```

### Saque Mensal (dia 5 de cada mês)
```json
{
  "transferInterval": "Monthly",
  "transferDay": 5
}
```

### Saque Mensal (dia 15 de cada mês)
```json
{
  "transferInterval": "Monthly",
  "transferDay": 15
}
```

---

## Sugestão de UI

### Select para Intervalo
```html
<select name="transferInterval">
  <option value="Daily">Diário (todo dia útil)</option>
  <option value="Weekly">Semanal</option>
  <option value="Monthly">Mensal</option>
</select>
```

### Select para Dia (condicional)

**Se `transferInterval = "Daily"`:**
- Ocultar o campo `transferDay` (ou setar fixo como 0)

**Se `transferInterval = "Weekly"`:**
```html
<select name="transferDay">
  <option value="0">Domingo</option>
  <option value="1">Segunda-feira</option>
  <option value="2">Terça-feira</option>
  <option value="3">Quarta-feira</option>
  <option value="4">Quinta-feira</option>
  <option value="5">Sexta-feira</option>
  <option value="6">Sábado</option>
</select>
```

**Se `transferInterval = "Monthly"`:**
```html
<select name="transferDay">
  <option value="1">Dia 1</option>
  <option value="2">Dia 2</option>
  ...
  <option value="31">Dia 31</option>
</select>
```

---

## Observações

1. **Transferência sempre automática**: O saque é sempre automático, o usuário escolhe apenas a frequência e o dia.

2. **Validação no backend**: O backend valida e ajusta valores inválidos:
   - Weekly com dia > 6 → ajusta para 1 (segunda)
   - Monthly com dia > 31 ou < 1 → ajusta para 1

3. **Campos opcionais**: Se não enviados, assume `Daily` com dia `0`.

4. **Atualização**: Pode ser alterado via `PUT /api/bank-accounts` a qualquer momento.
