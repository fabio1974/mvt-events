# Tipos de Entrega: CONTRACT vs ON_DEMAND vs Contrato Público

## 📋 Visão Geral dos 3 Cenários

### Cenário A: Contrato Privado (CONTRACT) - **MAIS COMUM** ⭐

```
┌──────────────┐                                    
│ CLIENT       │ (Cliente privado - pessoa física/empresa)
│ (privado)    │                                    
└──────┬───────┘                                    
       │                                            
       │ service_contract (contrato de serviço)    
       │                                            
       ▼                                            
┌──────────────┐                                    
│ ORGANIZER    │ (Organização de motoboys)         
└──────┬───────┘                                    
       │                                            
       │ Cria entrega                               
       ▼                                            
┌──────────────────────────────┐                    
│ DELIVERY                     │                    
│ - delivery_type: CONTRACT    │                    
│ - partnership_id: NULL       │ ← Contrato privado!
│ - client: CLIENT privado     │                    
└──────────────────────────────┘                    
       │                                            
       │ Notificação                                
       ▼                                            
┌──────────────────────────────┐                    
│ COURIERs da organização      │                    
└──────────────────────────────┘                    
```

**Características:**
- 🏢 **Cliente privado** (pessoa física ou empresa particular)
- 📝 Possui **service_contract** com ORGANIZER
- 🔗 `partnership_id = NULL` (não envolve prefeitura)
- 🎯 Notificação apenas para COURIERs da organização contratada
- 💰 Preço negociado no contrato de serviço

**Exemplo Real:**
- Restaurante "Pizza Express" → Contrata organização "Moto Rápida"
- Quando cliente solicita entrega → vai para motoboys da "Moto Rápida"

---

### Cenário B: Entrega Avulsa (ON_DEMAND) - **NOVO MODELO** 🆕

```
┌──────────────┐                                    
│ CLIENT       │ (Cliente sem contrato)             
│ (sem contrato)│                                   
└──────┬───────┘                                    
       │                                            
       │ Cria entrega SEM contrato prévio          
       │                                            
       ▼                                            
┌──────────────────────────────┐                    
│ DELIVERY                     │                    
│ - delivery_type: ON_DEMAND   │                    
│ - partnership_id: NULL       │                    
│ - client: CLIENT sem contrato│                    
└──────────────────────────────┘                    
       │                                            
       │ Broadcast (raio de ação)                  
       ▼                                            
┌──────────────────────────────┐                    
│ TODOS os COURIERs próximos   │                    
│ (qualquer organização)       │                    
└──────────────────────────────┘                    
```

**Características:**
- 👤 Cliente **sem service_contract**
- 🔗 `partnership_id = NULL`
- 📡 Notificação para **TODOS** os COURIERs no raio (nível 3)
- 🏃 Primeiro a aceitar leva a entrega
- 💰 Preço dinâmico (calculado por distância)

**Exemplo Real:**
- Usuário novo no app solicita entrega uma vez
- Não quer assinar contrato mensal
- Qualquer motoboy próximo pode aceitar

---

### Cenário C: Contrato Público (PARTNERSHIP) - **RARO** 🏛️

```
┌──────────────────────────────┐                    
│ Municipal Partnership        │                    
│ (Prefeitura de São Paulo)    │                    
│ CNPJ: 12.345.678/0001-00     │                    
└──────┬───────────────────────┘                    
       │                                            
       │ Convênio/licitação                        
       │                                            
       ▼                                            
┌──────────────┐                                    
│ ORGANIZER    │ (Org contratada pela prefeitura)  
└──────┬───────┘                                    
       │                                            
       │ Cria entrega institucional                
       ▼                                            
┌──────────────────────────────┐                    
│ DELIVERY                     │                    
│ - delivery_type: CONTRACT    │                    
│ - partnership_id: 1          │ ← ID da prefeitura!
│ - client: usuário da prefeit │                    
└──────────────────────────────┘                    
       │                                            
       │ Notificação                                
       ▼                                            
┌──────────────────────────────┐                    
│ COURIERs contratados         │                    
│ (licitação pública)          │                    
└──────────────────────────────┘                    
```

**Características:**
- 🏛️ **Prefeitura/Órgão Público** como cliente
- 📜 Contrato via **licitação/convênio**
- 🔗 `partnership_id != NULL` (aponta para municipal_partnerships)
- 🎯 COURIERs contratados especificamente para a prefeitura
- 💰 Valores tabelados (edital/convênio)

**Exemplo Real:**
- Prefeitura de São Paulo contrata sistema para entregar documentos
- Servidores da prefeitura solicitam entregas
- Apenas motoboys licitados podem executar

---

## 🔍 Comparação Lado a Lado

| Aspecto | Contrato Privado | ON_DEMAND | Contrato Público |
|---------|------------------|-----------|------------------|
| **Cliente** | Empresa/Pessoa privada | Qualquer usuário | Prefeitura/Órgão |
| **Contrato** | service_contract | Nenhum | municipal_partnership |
| **partnership_id** | `NULL` | `NULL` | `NOT NULL` |
| **delivery_type** | `CONTRACT` | `ON_DEMAND` | `CONTRACT` |
| **Notificação** | Org específica | Todos no raio | Org licitada |
| **Frequência** | ⭐⭐⭐⭐⭐ Muito comum | ⭐⭐⭐ Comum | ⭐ Raro |
| **Preço** | Negociado | Dinâmico | Tabelado |

---

## 📊 Como Diferenciar no Código

### Verificando o Tipo de Entrega:

```java
if (delivery.getDeliveryType() == DeliveryType.CONTRACT) {
    if (delivery.getPartnership() != null) {
        // CENÁRIO C: Contrato Público (prefeitura)
        System.out.println("Entrega institucional - Prefeitura");
    } else {
        // CENÁRIO A: Contrato Privado (service_contract)
        System.out.println("Entrega com contrato privado");
    }
} else if (delivery.getDeliveryType() == DeliveryType.ON_DEMAND) {
    // CENÁRIO B: Entrega Avulsa
    System.out.println("Entrega sob demanda");
}
```

### Query para Listar por Tipo:

```sql
-- Contratos Privados (mais comum)
SELECT * FROM deliveries 
WHERE delivery_type = 'CONTRACT' 
  AND partnership_id IS NULL;

-- Entregas Avulsas
SELECT * FROM deliveries 
WHERE delivery_type = 'ON_DEMAND';

-- Contratos Públicos (raro)
SELECT * FROM deliveries 
WHERE delivery_type = 'CONTRACT' 
  AND partnership_id IS NOT NULL;
```

---

## 🎯 Estatísticas Esperadas

Em um sistema típico:

```
📦 100 entregas totais

├─ 85 entregas: Contrato Privado (85%)
│  └─ service_contract com organizações
│
├─ 14 entregas: ON_DEMAND (14%)
│  └─ Clientes sem contrato
│
└─ 1 entrega: Contrato Público (1%)
   └─ Prefeitura/órgão público
```

---

## 💡 Regras de Negócio

### Criação de Entrega:

```java
if (client.hasActiveServiceContract()) {
    // Cenário A: Contrato Privado
    delivery.setDeliveryType(DeliveryType.CONTRACT);
    delivery.setPartnership(null);
    
} else if (client.isMunicipalEmployee()) {
    // Cenário C: Contrato Público
    delivery.setDeliveryType(DeliveryType.CONTRACT);
    delivery.setPartnership(client.getMunicipalPartnership());
    
} else {
    // Cenário B: ON_DEMAND
    delivery.setDeliveryType(DeliveryType.ON_DEMAND);
    delivery.setPartnership(null);
}
```

### Notificação de COURIERs:

```java
if (delivery.getDeliveryType() == DeliveryType.CONTRACT) {
    if (delivery.getPartnership() != null) {
        // Contrato Público: COURIERs licitados
        notifyLicitatedCouriers(delivery.getPartnership());
    } else {
        // Contrato Privado: COURIERs da organização
        notifyOrganizationCouriers(client.getServiceContract().getOrganization());
    }
} else {
    // ON_DEMAND: Todos no raio
    notifyAllCouriersInRadius(delivery.getFromLat(), delivery.getFromLng(), 10.0);
}
```

---

## 📝 Resumo Final

| Nome | Uso | partnership_id | delivery_type |
|------|-----|----------------|---------------|
| **Contrato Privado** | 🏢 Empresa → Organização | `NULL` | `CONTRACT` |
| **ON_DEMAND** | 👤 Sem contrato | `NULL` | `ON_DEMAND` |
| **Contrato Público** | 🏛️ Prefeitura → Licitação | `NOT NULL` | `CONTRACT` |

**Conclusão:** `partnership_id` é usado APENAS para prefeituras/órgãos públicos (Cenário C). Para o dia-a-dia do sistema (Cenários A e B), ele fica `NULL`! ✅
