# Correção: Billing Address - Adicionar Cartão

## Problema
Ao adicionar cartão com billing address, o backend retorna erro 400 com validação:

```json
{
  "fieldErrors": {
    "billingAddress.line1": "line_1 é obrigatório",
    "billingAddress.zipCode": "zip_code é obrigatório",
    "billingAddress.state": "state deve conter exatamente 2 letras maiúsculas"
  }
}
```

## Causa
O mobile está enviando o payload no formato **snake_case** (formato do Pagar.me), mas o backend espera **camelCase** (padrão Java).

## Correções Necessárias

### 1. Nomenclatura dos Campos
| ❌ Enviando (errado) | ✅ Deve enviar (correto) |
|---------------------|-------------------------|
| `line_1`            | `line1`                 |
| `line_2`            | `line2`                 |
| `zip_code`          | `zipCode`               |

### 2. Formato do Estado ⚠️ **ATENÇÃO: APENAS 2 LETRAS**
| ❌ Enviando (errado) | ✅ Deve enviar (correto) |
|---------------------|-------------------------|
| `"state": "Ceará"`  | `"state": "CE"`         |
| `"state": "CEARÁ"`  | `"state": "CE"`         |
| `"state": "Ceara"`  | `"state": "CE"`         |

O campo `state` deve conter **APENAS a sigla do estado**: exatamente **2 letras maiúsculas**.

❌ **NÃO ENVIAR**: Nome completo, acentuado, ou com mais de 2 caracteres  
✅ **ENVIAR**: Apenas as 2 letras da sigla (CE, SP, RJ, etc)

## Payload Correto

```json
{
  "cardToken": "token_86mZLl7fPfbGARG0",
  "setAsDefault": true,
  "billingAddress": {
    "line1": "129, Rua 31 de dezembro , Centro",
    "zipCode": "62350000",
    "city": "Ubajara",
    "state": "CE",
    "country": "BR"
  }
}
```

## Mapeamento Completo de Estados Brasileiros

| Estado | Sigla (usar) |
|--------|--------------|
| Acre | `AC` |
| Alagoas | `AL` |
| Amapá | `AP` |
| Amazonas | `AM` |
| Bahia | `BA` |
| Ceará | `CE` |
| Distrito Federal | `DF` |
| Espírito Santo | `ES` |
| Goiás | `GO` |
| Maranhão | `MA` |
| Mato Grosso | `MT` |
| Mato Grosso do Sul | `MS` |
| Minas Gerais | `MG` |
| Pará | `PA` |
| Paraíba | `PB` |
| Paraná | `PR` |
| Pernambuco | `PE` |
| Piauí | `PI` |
| Rio de Janeiro | `RJ` |
| Rio Grande do Norte | `RN` |
| Rio Grande do Sul | `RS` |
| Rondônia | `RO` |
| Roraima | `RR` |
| Santa Catarina | `SC` |
| São Paulo | `SP` |
| Sergipe | `SE` |
| Tocantins | `TO` |

## Observação
O backend converte automaticamente de camelCase para snake_case ao enviar para o Pagar.me. O mobile deve sempre enviar em camelCase.
