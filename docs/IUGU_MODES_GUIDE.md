# 🔧 Sistema de Modos Iugu - Dry-Run, Sandbox e Production

**Data**: 04/12/2025  
**Versão**: 1.0  
**Status**: ✅ Implementado

---

## 🎯 Visão Geral

O sistema de pagamentos agora suporta **3 modos de operação** controlados pela variável de ambiente `IUGU_MODE`:

| Modo | Descrição | API Key | Uso |
|------|-----------|---------|-----|
| 🧪 **dry-run** | Mock local (não chama Iugu) | Qualquer valor | Desenvolvimento local sem Iugu |
| 🏖️ **sandbox** | Iugu Sandbox (teste) | `test_xxx` | Testes com Iugu de teste |
| 🚀 **production** | Iugu Production (real) | `live_xxx` | Produção com transações reais |

---

## 📋 Configuração

### Variável de Ambiente

```bash
# application.properties
iugu.mode=${IUGU_MODE:dry-run}
```

### Modo 1: 🧪 DRY-RUN (Padrão)

**Mock local sem chamar Iugu**

```bash
# Não precisa definir nada (padrão)
# OU explicitamente:
export IUGU_MODE=dry-run

# Iniciar aplicação
./gradlew bootRun
```

**Características:**
- ✅ Não requer API Key válida do Iugu
- ✅ Retorna dados mockados (QR Code fake)
- ✅ Perfeito para desenvolvimento local
- ✅ Splits calculados corretamente
- ✅ Logs detalhados como se fosse real
- ⚠️ Nenhum pagamento real é criado no Iugu

**Response Mock:**
```json
{
  "paymentId": 1,
  "iuguInvoiceId": "MOCK_INV_1733360018123",
  "pixQrCode": "00020126360014BR.GOV.BCB.PIX0114+5511999999999...",
  "pixQrCodeUrl": "https://via.placeholder.com/300x300.png?text=QR+CODE+MOCK",
  "secureUrl": "https://mock.iugu.com/invoice/MOCK_INV_1733360018123",
  "amount": 25.00,
  "status": "PENDING"
}
```

---

### Modo 2: 🏖️ SANDBOX

**Iugu Sandbox para testes**

```bash
# Defina a API Key de teste do Iugu
export IUGU_MODE=sandbox
export IUGU_API_KEY=test_SUA_API_KEY_TESTE_AQUI

# Iniciar aplicação
./gradlew bootRun
```

**Características:**
- ✅ Chama API do Iugu **em modo teste**
- ✅ Pagamentos simulados (não cobram de verdade)
- ✅ QR Codes reais do Iugu Sandbox
- ✅ Testa integração completa sem cobranças
- ⚠️ Requer conta no Iugu e API Key `test_xxx`

**Como Obter API Key de Teste:**
1. Crie conta em https://iugu.com
2. Vá em **Administração** → **Configurações** → **API Tokens**
3. Copie a **Test API Key** (começa com `test_`)

---

### Modo 3: 🚀 PRODUCTION

**Iugu Production - transações reais**

```bash
# Defina a API Key de produção do Iugu
export IUGU_MODE=production
export IUGU_API_KEY=live_SUA_API_KEY_PRODUCAO_AQUI

# Iniciar aplicação
./gradlew bootRun
```

**Características:**
- ✅ Chama API do Iugu **em modo real**
- ⚠️ **Pagamentos reais com cobrança**
- ⚠️ QR Codes reais que cobram dos clientes
- ⚠️ Splits distribuídos para contas reais
- ⚠️ Requer API Key `live_xxx`

**⚠️ ATENÇÃO:**
- Apenas use em produção após testes completos
- Certifique-se de que as contas Iugu dos motoboys/gerentes estão validadas
- Monitore os logs e webhooks cuidadosamente

---

## 🔄 Mudança Entre Modos

### Desenvolvimento → Sandbox

```bash
# 1. Parar servidor
pkill -f 'java.*mvt-events'

# 2. Mudar para sandbox
export IUGU_MODE=sandbox
export IUGU_API_KEY=test_YOUR_TEST_API_KEY

# 3. Reiniciar
./gradlew bootRun
```

### Sandbox → Production

```bash
# 1. Parar servidor
pkill -f 'java.*mvt-events'

# 2. Mudar para production
export IUGU_MODE=production
export IUGU_API_KEY=live_YOUR_PRODUCTION_API_KEY

# 3. Reiniciar
./gradlew bootRun
```

### Production → Dry-Run (Emergência)

```bash
# Se precisar desabilitar rapidamente
export IUGU_MODE=dry-run
# Reiniciar servidor
```

---

## 📊 Logs de Inicialização

### Dry-Run
```
════════════════════════════════════════════════════════════
🧪 IUGU MODE: DRY_RUN
════════════════════════════════════════════════════════════
⚠️  ATENÇÃO: Modo DRY-RUN ativo!
   Faturas serão MOCKADAS e não enviadas ao Iugu
   Use IUGU_MODE=sandbox ou IUGU_MODE=production para integração real
────────────────────────────────────────────────────────────
```

### Sandbox
```
════════════════════════════════════════════════════════════
🏖️  IUGU MODE: SANDBOX
════════════════════════════════════════════════════════════
🏖️  Modo SANDBOX: Usando Iugu de teste
   API Key deve começar com 'test_'
────────────────────────────────────────────────────────────
```

### Production
```
════════════════════════════════════════════════════════════
🚀 IUGU MODE: PRODUCTION
════════════════════════════════════════════════════════════
🚀 Modo PRODUCTION: Usando Iugu REAL
   ⚠️  ATENÇÃO: Transações reais serão cobradas!
────────────────────────────────────────────────────────────
```

---

## 🧪 Logs Durante Criação de Invoice

### Dry-Run
```
🧪 DRY-RUN MODE: Criando fatura MOCK (não será enviada ao Iugu)
📝 Mock Invoice criada:
   🆔 ID: MOCK_INV_1733360018123
   💰 Valor: 2500¢ (R$ 25.00)
   📧 Email: cliente@example.com
   📦 Splits: 4 recipient(s)
   🔗 Secure URL: https://mock.iugu.com/invoice/MOCK_INV_1733360018123
   ⚠️  Este é um pagamento SIMULADO - nenhum valor real será cobrado
```

### Sandbox/Production
```
📝 Criando fatura Iugu - Email: cliente@example.com, Due Date: 2025-12-05...
POST https://api.iugu.com/v1/invoices - Criando invoice
✅ Fatura criada com sucesso: A1B2C3D4E5F6
```

---

## 📝 Códigos de Exemplo

### Verificar Modo no Código

```java
@Autowired
private IuguConfig iuguConfig;

public void someMethod() {
    if (iuguConfig.isDryRun()) {
        // Modo mock - não faz integração real
        log.info("Modo DRY-RUN: pulando webhook");
        return;
    }
    
    if (iuguConfig.isSandbox()) {
        log.info("Modo SANDBOX: processando webhook de teste");
    }
    
    if (iuguConfig.isProduction()) {
        log.info("Modo PRODUCTION: processando webhook real");
    }
}
```

### Enum de Modos

```java
IuguConfig.IuguMode mode = iuguConfig.getModeEnum();

switch (mode) {
    case DRY_RUN -> handleMockPayment();
    case SANDBOX -> handleTestPayment();
    case PRODUCTION -> handleRealPayment();
}
```

---

## ✅ Testes Recomendados

### 1. Teste em Dry-Run
```bash
export IUGU_MODE=dry-run
./gradlew bootRun

# Fazer POST /api/payment/create-invoice
# Verificar que retorna mock sem chamar Iugu
```

### 2. Teste em Sandbox
```bash
export IUGU_MODE=sandbox
export IUGU_API_KEY=test_...
./gradlew bootRun

# Fazer POST /api/payment/create-invoice
# Verificar que chama Iugu Sandbox
# Tentar pagar o QR Code (não cobra de verdade)
```

### 3. Teste em Production (Cuidado!)
```bash
export IUGU_MODE=production
export IUGU_API_KEY=live_...
./gradlew bootRun

# Fazer POST com valores pequenos
# Monitorar dashboard Iugu
# Confirmar splits corretos
```

---

## 🚨 Troubleshooting

### Erro: "401 UNAUTHORIZED"

**Dry-Run:** Não deveria acontecer (não chama API)  
**Sandbox/Production:** API Key inválida

```bash
# Verificar API Key
echo $IUGU_API_KEY

# Sandbox deve começar com test_
# Production deve começar com live_
```

### Modo não muda

```bash
# 1. Reiniciar servidor após mudar variável
pkill -f 'java.*mvt-events'
export IUGU_MODE=sandbox
./gradlew bootRun

# 2. Verificar logs de inicialização
tail -100 app-boot-shipping-fee.log | grep "IUGU MODE"
```

### QR Code não funciona em Sandbox

- ✅ Normal! QR Codes de sandbox são apenas para teste
- ✅ Não funcionam em apps bancários reais
- ✅ Use o simulador do Iugu Dashboard

---

## 📚 Referências

- [Documentação Iugu API](https://dev.iugu.com/reference/api-overview)
- [Iugu Marketplace](https://dev.iugu.com/docs/marketplace)
- [Testando Pagamentos Iugu](https://dev.iugu.com/docs/testando-pagamentos)

---

## ✅ Checklist de Deploy

- [ ] Testado em modo dry-run localmente
- [ ] Testado em modo sandbox com API Key de teste
- [ ] Splits validados em sandbox
- [ ] Webhooks configurados
- [ ] Contas Iugu dos motoboys/gerentes validadas
- [ ] API Key de produção obtida
- [ ] `IUGU_MODE=production` configurado no servidor
- [ ] Monitoramento ativo (logs, dashboard Iugu)
- [ ] Plano de rollback preparado

---

**Versão**: 1.0  
**Última atualização**: 04/12/2025  
**Status**: ✅ Sistema de modos implementado e testado
