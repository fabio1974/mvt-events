# 📱 Guia de Implementação Mobile - Pagamento Automático de Delivery (CUSTOMER)

## 📋 Índice
1. [Visão Geral](#visão-geral)
2. [Regras de Negócio](#regras-de-negócio)
3. [Fluxo de Cobrança Automática](#fluxo-de-cobrança-automática)
4. [Endpoints da API](#endpoints-da-api)
5. [Exemplos de Código (React Native)](#exemplos-de-código-react-native)
6. [Tratamento de Erros](#tratamento-de-erros)
7. [Push Notifications (FCM/Expo)](#push-notifications-fcmexpo)

---

## 🎯 Visão Geral

Este documento descreve a implementação do **pagamento automático de delivery para CUSTOMER** (cliente que abre a entrega diretamente no app).

### Características do Pagamento CUSTOMER:
- ✅ **Cobrança 100% automática** - sem interação do cliente no momento do pagamento
- ✅ Método de pagamento **pré-definido** no app (PIX ou Cartão default)
- ✅ Cartão sempre em **parcela única**
- ✅ Split: **87% motoboy** | **13% plataforma** (sem organizer)
- ✅ Momento da cobrança depende do **DeliveryType**

### ⚡ Fluxo Simplificado

```
┌─────────────────────────────────────────────────────────────────┐
│  COBRANÇA AUTOMÁTICA - SEM INTERAÇÃO DO CUSTOMER               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Customer já tem método de pagamento preferido no app       │
│     (PIX ou Cartão marcado como default)                       │
│                                                                 │
│  2. Quando motoboy aceita (DELIVERY) ou inicia viagem (RIDE)   │
│     → Backend cobra automaticamente usando o método preferido  │
│                                                                 │
│  3. Customer recebe apenas notificação de sucesso/falha        │
│     → Não precisa abrir nenhuma tela!                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📜 Regras de Negócio

### Tipos de Delivery e Momento da Cobrança Automática

| DeliveryType | Descrição | Quando Cobrar Automaticamente | Evento Backend |
|--------------|-----------|-------------------------------|----------------|
| `DELIVERY` | Entrega de objeto (comida, pacote) | Quando motoboy **aceita** a entrega | `ON_ACCEPT` |
| `RIDE` | Viagem de passageiro (tipo Uber) | Quando motoboy **inicia a viagem** | `ON_TRANSIT_START` |

### Método de Pagamento Preferido

O CUSTOMER deve ter configurado previamente no app:

| Preferência | Como é usado | Fallback |
|-------------|--------------|----------|
| `CREDIT_CARD` (default) | Usa o cartão marcado como `isDefault: true` | Se não houver cartão default, erro |
| `PIX` | Gera QR Code automaticamente | N/A |

### Split de Pagamento (CUSTOMER - sem organizer)

```
┌────────────────────────────────────────────────────┐
│  CUSTOMER PAYMENT SPLIT                            │
├────────────────────────────────────────────────────┤
│                                                    │
│  💰 Valor Total: R$ 50,00                          │
│                                                    │
│  ┌──────────────────────────────────────────┐     │
│  │  🏍️  Motoboy/Motorista: 87%  = R$ 43,50  │     │
│  ├──────────────────────────────────────────┤     │
│  │  🏢  Plataforma: 13%         = R$ 6,50   │     │
│  └──────────────────────────────────────────┘     │
│                                                    │
│  ⚠️ Diferente de ORGANIZER que divide:             │
│     Courier 87% | Organizer 5% | Plataforma 8%    │
│                                                    │
└────────────────────────────────────────────────────┘
```

---

## 🔄 Fluxo de Cobrança Automática

### Fluxo para DELIVERY (Entrega de objeto)

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. CUSTOMER cria delivery no app                                 │
│    POST /api/deliveries                                          │
│    deliveryType: "DELIVERY"                                      │
│    preferredPaymentMethod: "CREDIT_CARD" // ou "PIX"            │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ 2. Delivery fica disponível para motoboys                        │
│    Status: PENDING                                               │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ 3. Motoboy ACEITA a entrega                                      │
│    PATCH /api/deliveries/{id}/accept                             │
│                                                                  │
│    🔄 Backend executa AUTOMATICAMENTE:                           │
│       1. Busca método preferido do customer                     │
│       2. Se CARTÃO: usa cartão default do customer              │
│       3. Se PIX: gera QR Code                                   │
│       4. Cobra no Pagar.me com split                            │
│       5. Atualiza delivery.paymentCaptured = true               │
└──────────────────────────────────────────────────────────────────┘
                              ↓
           ┌────────────────┴────────────────┐
           ↓                                 ↓
┌─────────────────────────┐     ┌─────────────────────────┐
│ ✅ PAGAMENTO OK          │     │ ❌ PAGAMENTO FALHOU      │
│                         │     │                         │
│ • Aceite é confirmado   │     │ • Aceite é bloqueado    │
│ • Status → ACCEPTED     │     │ • Status permanece      │
│ • Customer recebe push: │     │   PENDING               │
│   "Entrega aceita!      │     │ • Customer recebe push: │
│    Pagamento de R$50    │     │   "Falha no pagamento.  │
│    realizado"           │     │    Verifique seu cartão"│
│ • Motoboy pode seguir   │     │ • Motoboy NÃO aceita    │
└─────────────────────────┘     └─────────────────────────┘
```

### Fluxo para RIDE (Viagem de passageiro)

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. CUSTOMER cria viagem no app                                   │
│    POST /api/deliveries                                          │
│    deliveryType: "RIDE"                                          │
│    preferredPaymentMethod: "CREDIT_CARD" // ou "PIX"            │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ 2. Motorista ACEITA a viagem (SEM cobrança ainda!)               │
│    PATCH /api/deliveries/{id}/accept                             │
│    Status: PENDING → ACCEPTED                                    │
│                                                                  │
│    💡 RIDE: cobrança só acontece quando INICIA viagem            │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│ 3. Motorista chega no local e INICIA viagem                      │
│    PATCH /api/deliveries/{id}/pickup                             │
│                                                                  │
│    🔄 Backend executa AUTOMATICAMENTE:                           │
│       1. Busca método preferido do customer                     │
│       2. Cobra no Pagar.me com split                            │
│       3. Atualiza delivery.paymentCaptured = true               │
└──────────────────────────────────────────────────────────────────┘
                              ↓
           ┌────────────────┴────────────────┐
           ↓                                 ↓
┌─────────────────────────┐     ┌─────────────────────────┐
│ ✅ PAGAMENTO OK          │     │ ❌ PAGAMENTO FALHOU      │
│                         │     │                         │
│ • Viagem inicia         │     │ • Viagem NÃO inicia     │
│ • Status → IN_TRANSIT   │     │ • Status permanece      │
│ • Customer recebe push: │     │   ACCEPTED              │
│   "Viagem iniciada!     │     │ • Customer recebe push: │
│    Pagamento de R$50    │     │   "Falha no pagamento.  │
│    realizado"           │     │    Verifique seu cartão"│
└─────────────────────────┘     └─────────────────────────┘
```

### Caso Especial: PIX

Para PIX, o fluxo é ligeiramente diferente pois precisa aguardar confirmação:

```
┌──────────────────────────────────────────────────────────────────┐
│ FLUXO PIX (Preferência do Customer = PIX)                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 1. Motoboy aceita (DELIVERY) ou inicia viagem (RIDE)            │
│                                                                  │
│ 2. Backend gera QR Code PIX automaticamente                     │
│                                                                  │
│ 3. Customer recebe push: "Pague o PIX para confirmar"           │
│    + QR Code é exibido via deep link ou notificação rica        │
│                                                                  │
│ 4. Customer paga o PIX no app do banco                          │
│                                                                  │
│ 5. Webhook Pagar.me confirma → Backend atualiza status          │
│                                                                  │
│ 6. Motoboy recebe confirmação e pode prosseguir                 │
│                                                                  │
│ ⏱️ Timeout: 5 minutos para pagar. Se não pagar, cancela.        │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🔌 Endpoints da API

### 1. Definir Método de Pagamento Preferido

O CUSTOMER deve definir seu método preferido previamente no app:

```http
PUT /api/customers/{customerId}/payment-preference
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "preferredPaymentMethod": "CREDIT_CARD",  // ou "PIX"
  "defaultCardId": "card_xyz789"            // obrigatório se CREDIT_CARD
}
```

**Response 200:**
```json
{
  "success": true,
  "preferredPaymentMethod": "CREDIT_CARD",
  "defaultCard": {
    "id": "card_xyz789",
    "brand": "Visa",
    "lastFourDigits": "4242"
  }
}
```

### 2. Listar Cartões Salvos

```http
GET /api/customer-cards
Authorization: Bearer {jwt_token}
```

**Response 200:**
```json
[
  {
    "id": "uuid-do-registro",
    "pagarmeCardId": "card_abc123",
    "brand": "Visa",
    "lastFourDigits": "4242",
    "holderName": "JOAO SILVA",
    "expMonth": 12,
    "expYear": 2026,
    "isDefault": true
  }
]
```

### 3. Criar Delivery (com método preferido já definido)

```http
POST /api/deliveries
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "deliveryType": "DELIVERY",           // ou "RIDE"
  "pickupAddress": { ... },
  "deliveryAddress": { ... },
  "amount": 50.00
  // NÃO precisa enviar método de pagamento aqui!
  // Backend usa o preferredPaymentMethod do customer
}
```

**Response 201:**
```json
{
  "id": "uuid-da-delivery",
  "deliveryType": "DELIVERY",
  "status": "PENDING",
  "paymentCaptured": false,
  "amount": 50.00,
  "preferredPaymentMethod": "CREDIT_CARD"  // Herdado do customer
}
```

### 4. Obter Detalhes da Delivery

```http
GET /api/deliveries/{id}
Authorization: Bearer {jwt_token}
```

**Response 200:**
```json
{
  "id": "uuid-da-delivery",
  "deliveryType": "DELIVERY",        // ou "RIDE"
  "status": "PENDING",
  "paymentCaptured": false,          // ⚠️ Importante para saber se pode aceitar/iniciar
  "amount": 50.00,
  "estimatedDistance": 5.2,
  "pickupAddress": { ... },
  "deliveryAddress": { ... },
  "courier": null,                   // Quando aceito, terá dados do motoboy
  "customer": { ... }
}
```

---

## 💻 Exemplos de Código (React Native)

### Configuração de Preferência de Pagamento (Tela de Configurações)

```typescript
// screens/PaymentPreferenceScreen.tsx
import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, FlatList, Alert } from 'react-native';
import { api } from '../services/api';

interface Card {
  id: string;
  pagarmeCardId: string;
  brand: string;
  lastFourDigits: string;
  isDefault: boolean;
}

export const PaymentPreferenceScreen = () => {
  const [cards, setCards] = useState<Card[]>([]);
  const [preferredMethod, setPreferredMethod] = useState<'CREDIT_CARD' | 'PIX'>('CREDIT_CARD');
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadCards();
    loadPreference();
  }, []);

  const loadCards = async () => {
    const response = await api.get('/customer-cards');
    setCards(response.data);
    const defaultCard = response.data.find((c: Card) => c.isDefault);
    if (defaultCard) {
      setSelectedCardId(defaultCard.pagarmeCardId);
    }
  };

  const loadPreference = async () => {
    const response = await api.get('/customers/me/payment-preference');
    setPreferredMethod(response.data.preferredPaymentMethod);
    setSelectedCardId(response.data.defaultCardId);
  };

  const savePreference = async () => {
    setLoading(true);
    try {
      await api.put('/customers/me/payment-preference', {
        preferredPaymentMethod: preferredMethod,
        defaultCardId: preferredMethod === 'CREDIT_CARD' ? selectedCardId : null,
      });
      Alert.alert('Sucesso', 'Preferência de pagamento salva!');
    } catch (error) {
      Alert.alert('Erro', 'Não foi possível salvar a preferência');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={{ flex: 1, padding: 20 }}>
      <Text style={{ fontSize: 20, fontWeight: 'bold', marginBottom: 20 }}>
        Método de Pagamento Preferido
      </Text>

      {/* Seletor de método */}
      <View style={{ flexDirection: 'row', marginBottom: 20 }}>
        <TouchableOpacity
          style={{
            flex: 1,
            padding: 15,
            backgroundColor: preferredMethod === 'CREDIT_CARD' ? '#007AFF' : '#E0E0E0',
            borderRadius: 8,
            marginRight: 10,
          }}
          onPress={() => setPreferredMethod('CREDIT_CARD')}
        >
          <Text style={{ color: preferredMethod === 'CREDIT_CARD' ? '#FFF' : '#000', textAlign: 'center' }}>
            💳 Cartão de Crédito
          </Text>
        </TouchableOpacity>
        
        <TouchableOpacity
          style={{
            flex: 1,
            padding: 15,
            backgroundColor: preferredMethod === 'PIX' ? '#007AFF' : '#E0E0E0',
            borderRadius: 8,
          }}
          onPress={() => setPreferredMethod('PIX')}
        >
          <Text style={{ color: preferredMethod === 'PIX' ? '#FFF' : '#000', textAlign: 'center' }}>
            📱 PIX
          </Text>
        </TouchableOpacity>
      </View>

      {/* Lista de cartões (se método = CREDIT_CARD) */}
      {preferredMethod === 'CREDIT_CARD' && (
        <>
          <Text style={{ fontSize: 16, marginBottom: 10 }}>
            Selecione o cartão para cobrança automática:
          </Text>
          <FlatList
            data={cards}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={{
                  padding: 15,
                  borderWidth: 2,
                  borderColor: selectedCardId === item.pagarmeCardId ? '#007AFF' : '#E0E0E0',
                  borderRadius: 8,
                  marginBottom: 10,
                }}
                onPress={() => setSelectedCardId(item.pagarmeCardId)}
              >
                <Text style={{ fontWeight: 'bold' }}>
                  {item.brand} •••• {item.lastFourDigits}
                </Text>
              </TouchableOpacity>
            )}
          />
        </>
      )}

      {/* Info sobre PIX */}
      {preferredMethod === 'PIX' && (
        <View style={{ backgroundColor: '#FFF3CD', padding: 15, borderRadius: 8, marginBottom: 20 }}>
          <Text style={{ color: '#856404' }}>
            ⚠️ Com PIX, você receberá uma notificação para pagar quando o motoboy aceitar/iniciar.
            O pagamento deve ser feito em até 5 minutos.
          </Text>
        </View>
      )}

      {/* Botão salvar */}
      <TouchableOpacity
        style={{
          backgroundColor: '#28A745',
          padding: 18,
          borderRadius: 8,
          marginTop: 'auto',
        }}
        onPress={savePreference}
        disabled={loading}
      >
        <Text style={{ color: '#FFF', textAlign: 'center', fontSize: 16, fontWeight: 'bold' }}>
          {loading ? 'Salvando...' : 'Salvar Preferência'}
        </Text>
      </TouchableOpacity>
    </View>
  );
};
```

### Push Notification Handler para Pagamentos

```typescript
// hooks/usePaymentPushNotifications.ts
import { useEffect } from 'react';
import { Alert } from 'react-native';
import * as Notifications from 'expo-notifications';
import { useNavigation } from '@react-navigation/native';

/**
 * Hook para processar Push Notifications de pagamento automático
 * Usa Expo Push Notifications (FCM por baixo no Android)
 * O customer não precisa fazer nada - apenas receber a notificação
 */
export const usePaymentPushNotifications = () => {
  const navigation = useNavigation();

  useEffect(() => {
    // Listener para notificações recebidas com app em foreground
    const foregroundSubscription = Notifications.addNotificationReceivedListener(
      (notification) => {
        const data = notification.request.content.data;
        handlePaymentNotification(data);
      }
    );

    // Listener para quando usuário toca na notificação
    const responseSubscription = Notifications.addNotificationResponseReceivedListener(
      (response) => {
        const data = response.notification.request.content.data;
        handlePaymentNotificationTap(data, navigation);
      }
    );

    return () => {
      foregroundSubscription.remove();
      responseSubscription.remove();
    };
  }, [navigation]);
};

/**
 * Processa notificação de pagamento recebida
 */
const handlePaymentNotification = (data: any) => {
  const { type, deliveryId, amount, code, message } = data;

  switch (type) {
    case 'PAYMENT_SUCCESS':
      // ✅ Pagamento automático bem sucedido (Cartão)
      Alert.alert(
        '✅ Pagamento Realizado!',
        `Seu pagamento de R$ ${parseFloat(amount).toFixed(2)} foi processado automaticamente.`,
        [{ text: 'OK' }]
      );
      break;

    case 'PAYMENT_FAILED':
      // ❌ Pagamento automático falhou
      Alert.alert(
        '❌ Falha no Pagamento',
        message || 'Não foi possível processar o pagamento. Verifique seu cartão.',
        [{ text: 'OK' }]
      );
      break;

    case 'PIX_REQUIRED':
      // 📱 PIX: Precisa pagar manualmente (tratado no tap)
      console.log('PIX required - user will tap notification');
      break;

    case 'PIX_CONFIRMED':
      // ✅ PIX confirmado
      Alert.alert('✅ PIX Confirmado!', 'Seu pagamento foi recebido.');
      break;
  }
};

/**
 * Processa tap na notificação de pagamento
 */
const handlePaymentNotificationTap = (data: any, navigation: any) => {
  const { type, deliveryId } = data;

  switch (type) {
    case 'PAYMENT_FAILED':
      // Redireciona para configurações de pagamento
      navigation.navigate('PaymentPreference');
      break;

    case 'PIX_REQUIRED':
      // Abre tela de PIX com QR Code
      navigation.navigate('PixPaymentScreen', {
        qrCode: data.pixQrCode,
        qrCodeUrl: data.pixQrCodeUrl,
        amount: data.amount,
        expiresAt: data.pixExpiresAt,
        deliveryId: deliveryId,
      });
      break;

    default:
      // Vai para detalhes da entrega
      if (deliveryId) {
        navigation.navigate('DeliveryDetails', { deliveryId });
      }
  }
};
```

### Tela de PIX (quando preferência é PIX)

```typescript
// screens/PixPaymentScreen.tsx
import React, { useEffect, useState } from 'react';
import { View, Text, Image, TouchableOpacity, Alert, Clipboard } from 'react-native';
import { useRoute } from '@react-navigation/native';

export const PixPaymentScreen = () => {
  const route = useRoute();
  const { qrCode, qrCodeUrl, amount, expiresAt, deliveryId } = route.params as any;
  const [timeLeft, setTimeLeft] = useState(300); // 5 minutos

  useEffect(() => {
    const timer = setInterval(() => {
      const now = new Date();
      const expires = new Date(expiresAt);
      const diff = Math.max(0, Math.floor((expires.getTime() - now.getTime()) / 1000));
      setTimeLeft(diff);
      
      if (diff === 0) {
        Alert.alert('⏱️ Tempo Esgotado', 'O prazo para pagamento expirou.');
        clearInterval(timer);
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [expiresAt]);

  const formatTime = (seconds: number) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}:${sec.toString().padStart(2, '0')}`;
  };

  const copyToClipboard = () => {
    Clipboard.setString(qrCode);
    Alert.alert('✅ Copiado!', 'Código PIX copiado para a área de transferência');
  };

  return (
    <View style={{ flex: 1, padding: 20, alignItems: 'center' }}>
      <Text style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 10 }}>
        Pague com PIX
      </Text>
      
      <Text style={{ fontSize: 18, marginBottom: 20 }}>
        Valor: R$ {amount.toFixed(2)}
      </Text>

      <Text style={{ fontSize: 16, color: timeLeft < 60 ? 'red' : '#666', marginBottom: 20 }}>
        ⏱️ Tempo restante: {formatTime(timeLeft)}
      </Text>

      <Image
        source={{ uri: qrCodeUrl }}
        style={{ width: 250, height: 250, marginBottom: 20 }}
      />

      <TouchableOpacity
        style={{
          backgroundColor: '#007AFF',
          padding: 15,
          borderRadius: 8,
          width: '100%',
        }}
        onPress={copyToClipboard}
      >
        <Text style={{ color: '#FFF', textAlign: 'center', fontWeight: 'bold' }}>
          📋 Copiar Código PIX
        </Text>
      </TouchableOpacity>

      <Text style={{ marginTop: 20, textAlign: 'center', color: '#888' }}>
        Abra o app do seu banco e cole o código acima para pagar.
        A confirmação é automática!
      </Text>
    </View>
  );
};
```

### Hook para usar nas telas de Delivery

```typescript
// hooks/useDelivery.ts

/**
 * Uso no app do CUSTOMER:
 * 
 * O customer NÃO precisa fazer nada no momento do pagamento (exceto PIX).
 * Apenas escutar as notificações.
 */

import { usePaymentNotifications } from './usePaymentNotifications';

export const useDeliveryTracking = (deliveryId: string) => {
  // Escuta notificações de pagamento automático
  usePaymentNotifications(deliveryId);
  
  // ... resto da lógica de tracking
};
```

---

## ⚠️ Tratamento de Erros

### Códigos de Erro Comuns (recebidos via Push Notification)

| Código | Descrição | Ação no Mobile |
|--------|-----------|----------------|
| `NO_DEFAULT_CARD` | Nenhum cartão default definido | Redirecionar para tela de preferências |
| `CARD_EXPIRED` | Cartão expirado | Notificar e pedir para atualizar cartão |
| `INSUFFICIENT_FUNDS` | Saldo insuficiente | Notificar e sugerir outro cartão |
| `CARD_DECLINED` | Cartão recusado | Notificar e sugerir outro cartão |
| `PIX_TIMEOUT` | PIX não pago no prazo | Notificar que a delivery foi cancelada |
| `GATEWAY_ERROR` | Erro no Pagar.me | Notificar erro temporário |

### Handler de Notificação de Falha (Push Notification)

```typescript
// Exemplo: processar push de falha no pagamento
const handlePaymentFailedNotification = (data: any, navigation: any) => {
  const { code, message } = data;
  
  switch (code) {
    case 'NO_DEFAULT_CARD':
      Alert.alert(
        '⚠️ Cartão não configurado',
        'Configure um cartão de crédito como padrão para continuar.',
        [
          { text: 'Configurar', onPress: () => navigation.navigate('PaymentPreference') }
        ]
      );
      break;
      
    case 'CARD_EXPIRED':
    case 'CARD_DECLINED':
    case 'INSUFFICIENT_FUNDS':
      Alert.alert(
        '❌ Problema com seu cartão',
        message || 'Verifique seu cartão e tente novamente.',
        [
          { text: 'Alterar Cartão', onPress: () => navigation.navigate('PaymentPreference') }
        ]
      );
      break;
      
    default:
      Alert.alert('❌ Falha no Pagamento', message || 'Tente novamente.');
  }
};
```

---

## 🔔 Push Notifications (FCM/Expo)

> **✅ IMPLEMENTADO**: O backend já possui `PushNotificationService` que envia notificações via **Expo Push Notifications** (usa FCM para Android e APNs para iOS).

### Tipos de Notificação para o CUSTOMER

| Type (data.type) | Quando Ocorre | Payload | Ação no App |
|------------------|---------------|---------|-------------|
| `PAYMENT_SUCCESS` | Cartão cobrado com sucesso | `{ deliveryId, amount, paymentId, paymentMethod }` | Exibir Toast/Alert de sucesso |
| `PAYMENT_FAILED` | Cobrança automática falhou | `{ deliveryId, code, message }` | Exibir alerta + direcionar para configurações |
| `PIX_REQUIRED` | Precisa pagar PIX | `{ deliveryId, pixQrCode, pixQrCodeUrl, amount, pixExpiresAt }` | Abrir tela de PIX |
| `PIX_CONFIRMED` | PIX foi pago | `{ deliveryId, paymentId }` | Fechar tela PIX + notificação |
| `DELIVERY_STATUS_CHANGED` | Status mudou | `{ deliveryId, oldStatus, newStatus }` | Atualizar UI |

### Tipos de Notificação para o MOTOBOY

| Type (data.type) | Quando Ocorre | Payload | Ação no App |
|------------------|---------------|---------|-------------|
| `PAYMENT_CONFIRMED` | Pagamento do customer ok | `{ deliveryId, paymentId }` | Liberar ação (aceitar/iniciar) |
| `PAYMENT_WAITING` | Aguardando pagamento | `{ deliveryId, message }` | Exibir "Aguardando pagamento..." |

### Configuração no App (Expo)

```typescript
// App.tsx ou similar
import * as Notifications from 'expo-notifications';

// Configurar handler de notificação
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

// Obter e registrar token no backend
const registerPushToken = async () => {
  const { status } = await Notifications.requestPermissionsAsync();
  if (status !== 'granted') return;

  const token = await Notifications.getExpoPushTokenAsync();
  
  // Enviar token para o backend
  await api.post('/api/push-tokens', {
    token: token.data,
    platform: Platform.OS,
  });
};
```

### Backend: Como Enviar Notificação de Pagamento

```java
// No service de pagamento, após processar:
pushNotificationService.sendNotificationToUser(
    customerId,
    "✅ Pagamento Realizado",
    "Seu pagamento de R$ " + amount + " foi processado.",
    Map.of(
        "type", "PAYMENT_SUCCESS",
        "deliveryId", deliveryId.toString(),
        "amount", amount.toString(),
        "paymentMethod", "CREDIT_CARD"
    )
);
```

---

## 📝 Checklist de Implementação

### App do CUSTOMER

#### Push Notifications (pré-requisito)
- [ ] Configurar Expo Notifications no App.tsx
- [ ] Solicitar permissão de notificação
- [ ] Registrar push token no backend (`POST /api/push-tokens`)

#### Configuração de Preferência (uma vez)
- [ ] Tela de configuração de método preferido (PIX ou Cartão)
- [ ] Seleção de cartão default para cobrança automática
- [ ] Validação: se CREDIT_CARD, deve ter cartão default

#### Cobrança Automática (via Push Notification)
- [ ] Handler de push `PAYMENT_SUCCESS` (mostrar Toast/Alert)
- [ ] Handler de push `PAYMENT_FAILED` (direcionar para config)
- [ ] Handler de push `PIX_REQUIRED` (abrir tela PIX)
- [ ] Handler de push `PIX_CONFIRMED` (fechar tela PIX)
- [ ] Tela de PIX com QR Code, timer e botão copiar

### App do MOTOBOY/MOTORISTA
- [ ] Handler de push `PAYMENT_CONFIRMED` (liberar ação)
- [ ] Handler de push `PAYMENT_WAITING` (exibir aguardando)
- [ ] UI de "Aguardando pagamento do cliente..." quando backend processa

---

## 📞 Suporte

Dúvidas sobre a implementação? Consulte:
- [Documentação Pagar.me](https://docs.pagar.me)
- [DELIVERY_TYPE_PAYMENT_STRATEGY.md](./DELIVERY_TYPE_PAYMENT_STRATEGY.md) - Estratégia de pagamento por tipo
