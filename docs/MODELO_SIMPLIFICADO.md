# Tipos de Entrega: CONTRACT vs ON_DEMAND (Modelo Simplificado)

## 📋 Visão Geral dos 2 Cenários

### Cenário A: Entrega com Contrato (CONTRACT) - **COMUM** ⭐

```
┌──────────────┐                                    
│ CLIENT       │ (Cliente: privado OU público)      
└──────┬───────┘                                    
       │                                            
       │ service_contract                           
       │                                            
       ▼                                            
┌──────────────┐                                    
│ ORGANIZER    │ (Organização: privada OU prefeitura)
└──────┬───────┘                                    
       │                                            
       │ Entregas com contrato                     
       ▼                                            
┌──────────────────────────────┐                    
│ DELIVERY                     │                    
│ - delivery_type: CONTRACT    │                    
│ - client: tem service_contract│                   
└──────────────────────────────┘                    
       │                                            
       │ Notificação para COURIERs da org          
       ▼                                            
┌──────────────────────────────┐                    
│ COURIERs da organização      │                    
└──────────────────────────────┘                    
```

**Características:**
- 📝 CLIENT possui **service_contract** com ORGANIZER
- 🏢 ORGANIZER pode ser:
  - **Privado**: Empresa de entregas comum
  - **Público**: Prefeitura/órgão público cadastrado como ORGANIZER
- 🎯 Notificação apenas para COURIERs da organização contratada
- 💰 Preço negociado no contrato de serviço

**Exemplos Reais:**

**Privado:**
- Restaurante "Pizza Express" → Contrata "Moto Rápida"
- Loja "Magazine Luiza" → Contrata "Express Delivery"

**Público:**
- Servidor da Prefeitura SP → Contrata "Correios Municipal"
- Departamento de Saúde → Contrata organização licitada

---

### Cenário B: Entrega Avulsa (ON_DEMAND) - **NOVO MODELO** 🆕

```
┌──────────────┐                                    
│ CLIENT       │ (Qualquer pessoa sem contrato)     
└──────┬───────┘                                    
       │                                            
       │ Solicita entrega SEM contrato             
       │                                            
       ▼                                            
┌──────────────────────────────┐                    
│ DELIVERY                     │                    
│ - delivery_type: ON_DEMAND   │                    
│ - client: sem service_contract│                   
└──────────────────────────────┘                    
       │                                            
       │ Broadcast (raio de ação - nível 3)        
       ▼                                            
┌──────────────────────────────┐                    
│ TODOS os COURIERs próximos   │                    
│ (qualquer organização)       │                    
└──────────────────────────────┘                    
```

**Características:**
- 👤 Cliente **sem service_contract**
- 📡 Notificação para **TODOS** os COURIERs no raio
- 🏃 Primeiro a aceitar leva a entrega
- 💰 Preço dinâmico (calculado por distância)

**Exemplo Real:**
- Usuário novo no app solicita entrega única
- Não quer assinar contrato mensal
- Qualquer motoboy próximo pode aceitar

---

## 🔍 Comparação Lado a Lado

| Aspecto | CONTRACT | ON_DEMAND |
|---------|----------|-----------|
| **Cliente** | Qualquer (privado/público) | Qualquer usuário |
| **Tem Contrato?** | ✅ Sim (service_contract) | ❌ Não |
| **Organização** | Específica (privada ou pública) | Qualquer no raio |
| **delivery_type** | `CONTRACT` | `ON_DEMAND` |
| **Notificação** | COURIERs da org | Todos no raio |
| **Frequência** | ⭐⭐⭐⭐⭐ Muito comum | ⭐⭐⭐ Comum |
| **Preço** | Negociado (contrato) | Dinâmico (distância) |

---

## 🏛️ Como Prefeituras Usam o Sistema

### Modelo Simplificado:

```
1. Prefeitura de São Paulo se cadastra como ORGANIZER
   - Nome: "Prefeitura de São Paulo"
   - CNPJ: 12.345.678/0001-00
   - Tipo: Organização pública

2. Prefeitura contrata/licita motoboys
   - Motoboys cadastrados como COURIERs
   - Vinculados via employment_contract

3. Servidores públicos se cadastram como CLIENTs
   - Fazem service_contract com a prefeitura (ORGANIZER)
   
4. Servidor solicita entrega
   - Tipo: CONTRACT (tem service_contract)
   - Vai apenas para COURIERs da prefeitura
```

**Não há diferença técnica entre organização privada e pública!** 
Ambas são apenas ORGANIZERs no sistema. ✅

---

## 📊 Como Diferenciar no Código

### Verificando o Tipo de Entrega:

```java
if (delivery.getDeliveryType() == DeliveryType.CONTRACT) {
    // Cliente tem service_contract
    ServiceContract contract = serviceContractRepository
        .findActiveByClientId(delivery.getClient().getId())
        .orElseThrow();
    
    Organization org = contract.getOrganization();
    System.out.println("Entrega contratada com: " + org.getName());
    
    // Pode ser privado ou público (não importa!)
    notifyOrganizationCouriers(org);
    
} else if (delivery.getDeliveryType() == DeliveryType.ON_DEMAND) {
    // Cliente SEM service_contract
    System.out.println("Entrega sob demanda");
    notifyAllCouriersInRadius(delivery.getFromLat(), delivery.getFromLng());
}
```

### Query para Listar por Tipo:

```sql
-- Entregas com Contrato
SELECT 
    d.*,
    o.name as organization_name,
    o.cnpj as organization_cnpj
FROM deliveries d
JOIN users c ON c.id = d.client_id
JOIN service_contracts sc ON sc.client_id = c.id AND sc.status = 'ACTIVE'
JOIN organizations o ON o.id = sc.organization_id
WHERE d.delivery_type = 'CONTRACT';

-- Entregas Avulsas (sem contrato)
SELECT * FROM deliveries 
WHERE delivery_type = 'ON_DEMAND';
```

---

## 🎯 Estatísticas Esperadas

Em um sistema típico:

```
📦 100 entregas totais

├─ 85 entregas: CONTRACT (85%)
│  ├─ 75 entregas: Organizações privadas
│  └─ 10 entregas: Organizações públicas (prefeituras)
│
└─ 15 entregas: ON_DEMAND (15%)
   └─ Clientes sem contrato
```

---

## 💡 Regras de Negócio Simplificadas

### Criação de Entrega:

```java
public Delivery createDelivery(UUID clientId, DeliveryRequest request) {
    User client = userRepository.findById(clientId).orElseThrow();
    
    // Verifica se cliente tem contrato ativo
    Optional<ServiceContract> contract = serviceContractRepository
        .findActiveByClientId(clientId);
    
    Delivery delivery = new Delivery();
    delivery.setClient(client);
    
    if (contract.isPresent()) {
        // Tem contrato → CONTRACT
        delivery.setDeliveryType(DeliveryType.CONTRACT);
    } else {
        // Sem contrato → ON_DEMAND
        delivery.setDeliveryType(DeliveryType.ON_DEMAND);
    }
    
    // Preencher outros campos...
    delivery.setFromAddress(request.getFromAddress());
    delivery.setToAddress(request.getToAddress());
    delivery.setTotalAmount(request.getTotalAmount());
    
    return deliveryRepository.save(delivery);
}
```

### Notificação de COURIERs:

```java
public void notifyAvailableCouriers(Delivery delivery) {
    if (delivery.getDeliveryType() == DeliveryType.CONTRACT) {
        // Buscar organização via service_contract
        ServiceContract contract = serviceContractRepository
            .findActiveByClientId(delivery.getClient().getId())
            .orElseThrow();
        
        // Notificar apenas COURIERs desta organização
        List<User> couriers = employmentContractRepository
            .findActiveCouriersByOrganizationId(contract.getOrganization().getId());
        
        couriers.forEach(courier -> 
            sendPushNotification(courier, "Nova entrega disponível")
        );
        
    } else {
        // ON_DEMAND: Todos os COURIERs no raio (nível 3)
        double radiusKm = 10.0;
        List<User> nearbyCouriers = courierLocationService
            .findCouriersInRadius(
                delivery.getFromLat(), 
                delivery.getFromLng(), 
                radiusKm
            );
        
        nearbyCouriers.forEach(courier -> 
            sendPushNotification(courier, "🔥 Entrega avulsa no seu raio!")
        );
    }
}
```

---

## 📝 Resumo Final

### Antes (Complexo):
```
❌ 3 tipos: CONTRACT privado, ON_DEMAND, CONTRACT público
❌ Tabela municipal_partnerships
❌ Campo partnership_id
❌ Muita complexidade desnecessária
```

### Agora (Simples):
```
✅ 2 tipos: CONTRACT, ON_DEMAND
✅ ORGANIZER serve para público E privado
✅ Sem tabela municipal_partnerships
✅ Sem campo partnership_id
✅ Modelo limpo e direto
```

### Estrutura Final:

| Tabela | Propósito |
|--------|-----------|
| `users` | Todos os usuários (ADMIN, CLIENT, ORGANIZER, COURIER) |
| `organizations` | Organizações (privadas OU públicas) |
| `service_contracts` | Contratos CLIENT ↔ ORGANIZER |
| `employment_contracts` | Contratos COURIER ↔ ORGANIZER |
| `deliveries` | Entregas (CONTRACT ou ON_DEMAND) |

**Conclusão:** Sistema mais simples, mais fácil de manter, e igualmente poderoso! 🚀
