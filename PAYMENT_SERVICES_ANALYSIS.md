# Análise dos Serviços de Pagamento - Frontend

## 🔍 Situação Identificada

Existem **dois serviços de pagamento diferentes** no frontend (`mvt-fe`):

### 📁 `/src/services/api.ts`

```typescript
export const paymentService = {
  // Usa POST para calculateFee ❌
  async calculateFee(
    amount: number,
    provider: string
  ): Promise<FeeCalculationResponse> {
    const response = await api.post<FeeCalculationResponse>(
      "/payments/calculate-fee",
      {
        amount,
        provider,
      }
    );
    return response.data;
  },

  // Outros métodos...
};
```

### 📁 `/src/services/payment.ts`

```typescript
export const paymentService = {
  // Usa GET para calculateFee ✅
  async calculateFee(
    amount: number,
    paymentMethod: string
  ): Promise<FeeCalculation> {
    const response = await api.get("/payments/calculate-fee", {
      params: { amount, paymentMethod },
    });
    return response.data as FeeCalculation;
  },

  // Métodos mais completos e tipados...
};
```

## 🎯 **Backend Real - `/api/payments/calculate-fee`**

```java
@GetMapping("/calculate-fee")
public ResponseEntity<?> calculateFee(@RequestParam BigDecimal amount,
        @RequestParam Payment.PaymentMethod paymentMethod) {
    // Implementação real usa GET ✅
}
```

## ✅ **Resposta: `/src/services/payment.ts` é o CORRETO**

### **Razões:**

1. **Método HTTP Correto**:

   - ✅ `payment.ts` usa `GET` (conforme backend)
   - ❌ `api.ts` usa `POST` (incompatível)

2. **Parâmetros Corretos**:

   - ✅ `payment.ts` usa `paymentMethod` (conforme backend)
   - ❌ `api.ts` usa `provider` (parâmetro inexistente)

3. **Completude**:

   - ✅ `payment.ts` tem interface `FeeCalculation` completa
   - ❌ `api.ts` tem interface `FeeCalculationResponse` simples

4. **Tipagem Superior**:
   - ✅ `payment.ts` tem tipos mais detalhados (`PaymentMethod`, `PaymentRequest`, etc.)
   - ❌ `api.ts` tem tipos básicos

## 🔧 **Ação Recomendada**

### 1. **PaymentMethodSelector deve usar**:

```typescript
import { paymentService } from "../../services/payment"; // ✅ CORRETO
```

### 2. **Remover/Deprecar**:

```typescript
// ❌ Remover este serviço duplicado de api.ts
export const paymentService = { ... }
```

### 3. **Centralizar em payment.ts**:

- Manter apenas o `paymentService` do `payment.ts`
- Garantir que todos os componentes importem de `payment.ts`

## 📋 **Endpoints Backend vs Frontend**

| Endpoint                  | Backend | payment.ts | api.ts    | Status       |
| ------------------------- | ------- | ---------- | --------- | ------------ |
| `/payments/methods`       | `GET`   | `GET` ✅   | `GET` ✅  | OK           |
| `/payments/calculate-fee` | `GET`   | `GET` ✅   | `POST` ❌ | **Corrigir** |
| `/payments/create`        | `POST`  | `POST` ✅  | `POST` ✅ | OK           |

## 🎯 **Conclusão**

O **PaymentMethodSelector está importando corretamente** de `payment.ts`. O problema é que existe um serviço duplicado e inconsistente em `api.ts` que deve ser **removido** para evitar confusão.

**Ação:** Remover `paymentService` de `api.ts` e manter apenas em `payment.ts`.
