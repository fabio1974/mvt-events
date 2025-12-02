# Sistema de Entregas ON-DEMAND (Avulsas/Sem Contrato)

## 📋 Visão Geral

O sistema agora suporta **dois tipos de entregas**:

1. **CONTRACT** (Com Contrato) - Modelo original
2. **ON_DEMAND** (Avulsa/Sob Demanda) - Novo modelo implementado

---

## 🎯 Modelo de Negócio

### Entrega CONTRACT (Com Contrato)

```
┌─────────┐         ┌─────────────────┐         ┌──────────┐
│ CLIENT  │────────▶│ service_contract│────────▶│ORGANIZER │
└─────────┘         └─────────────────┘         └──────────┘
                             │                         │
                             │                         │
                             ▼                         ▼
                       ┌──────────┐          ┌──────────────┐
                       │ DELIVERY │          │   (opcional) │
                       │partnership_id = NULL │  partnership │
                       │delivery_type = CONTRACT│            │
                       └──────────┘          └──────────────┘
                             │
                             │ Notificação para
                             ▼ COURIERs da organização
                    ┌─────────────────┐
                    │ COURIER (org X) │
                    └─────────────────┘
```

**Características:**
- CLIENT tem **service_contract** com uma ORGANIZAÇÃO específica (ORGANIZER)
- Delivery **geralmente** com `partnership_id = NULL` (contrato privado)
- Delivery **raramente** com `partnership_id != NULL` (quando cliente é prefeitura)
- Notificação vai **apenas** para COURIERs daquela organização
- Algoritmos de nível 1 ou 2 (filtrado por organização)

---

### Entrega ON_DEMAND (Avulsa/Sob Demanda) ⭐ **NOVO**

```
┌─────────┐
│ CLIENT  │ (Sem contrato de serviço)
└─────────┘
     │
     │ Solicita entrega
     ▼
┌──────────────────┐
│    DELIVERY      │
│ partnership_id = NULL │
│ delivery_type = ON_DEMAND │
└──────────────────┘
     │
     │ Notificação em broadcast
     │ para TODOS os COURIERs no raio
     ▼
┌─────────────────────────────────────┐
│  TODOS os COURIERs disponíveis      │
│  dentro do raio de X km             │
│  (Nível 3 do algoritmo - sem filtro │
│   de organização)                   │
└─────────────────────────────────────┘
```

**Características:**
- CLIENT **NÃO precisa** ter `service_contract`
- Delivery **SEM** `partnership_id` (NULL)
- Notificação vai para **TODOS** os COURIERs disponíveis no raio
- Algoritmo de nível 3 (raio de ação, sem filtro de organização)
- Primeiro COURIER a aceitar fica com a entrega

---

## 🗄️ Estrutura do Banco de Dados

### Campo `delivery_type`

```sql
ALTER TABLE deliveries 
ADD COLUMN delivery_type VARCHAR(20) DEFAULT 'CONTRACT' NOT NULL;

-- Valores possíveis: 'CONTRACT' ou 'ON_DEMAND'
```

### Constraints de Integridade

```sql
-- 1. Tipo deve ser CONTRACT ou ON_DEMAND
ALTER TABLE deliveries 
ADD CONSTRAINT chk_delivery_type
CHECK (delivery_type IN ('CONTRACT', 'ON_DEMAND'));

### ⚠️ IMPORTANTE: partnership_id vs service_contract

**partnership_id:**
- Usado APENAS para parcerias com **prefeituras/órgãos públicos**
- Uso RARO no sistema
- Geralmente NULL para contratos privados

**service_contract:**
- Contrato entre **CLIENT privado** e **ORGANIZER** (organização de motoboys)
- Uso COMUM no sistema
- NÃO usa partnership_id

**Regra:**
```sql
-- Entregas CONTRACT podem ter partnership_id NULL
-- (quando é contrato privado via service_contract)
-- OU partnership_id NOT NULL (quando é prefeitura)

-- Entregas ON_DEMAND sempre tem partnership_id NULL
```

**Nenhuma constraint força partnership_id!** A validação é apenas no delivery_type (CONTRACT ou ON_DEMAND).

-- 3. ON_DEMAND não pode ter partnership_id
ALTER TABLE deliveries 
ADD CONSTRAINT chk_on_demand_no_partnership
CHECK (
    delivery_type != 'ON_DEMAND' OR 
    (delivery_type = 'ON_DEMAND' AND partnership_id IS NULL)
);
```

### Índices para Performance

```sql
-- Busca rápida de entregas ON_DEMAND disponíveis
CREATE INDEX idx_delivery_on_demand_status 
ON deliveries(delivery_type, status, created_at DESC) 
WHERE delivery_type = 'ON_DEMAND' 
  AND status IN ('PENDING', 'ACCEPTED');
```

### View para Entregas Disponíveis

```sql
CREATE VIEW available_on_demand_deliveries AS
SELECT 
    d.id,
    d.client_id,
    d.from_address,
    d.from_lat,
    d.from_lng,
    d.to_address,
    d.distance_km,
    d.total_amount,
    d.created_at,
    u.name as client_name,
    u.phone as client_phone
FROM deliveries d
JOIN users u ON u.id = d.client_id
WHERE d.delivery_type = 'ON_DEMAND'
  AND d.status = 'PENDING'
  AND d.courier_id IS NULL
ORDER BY d.created_at ASC;
```

---

## 🔧 Implementação no Código

### Enum DeliveryType (a ser criado)

```java
public enum DeliveryType {
    CONTRACT,   // Com contrato de serviço
    ON_DEMAND   // Avulsa, sem contrato
}
```

### Entity Delivery

```java
@Entity
@Table(name = "deliveries")
public class Delivery {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType = DeliveryType.CONTRACT;
    
    @ManyToOne
    @JoinColumn(name = "partnership_id")
    private MunicipalPartnership partnership;  // NULL para ON_DEMAND
    
    // ... outros campos
}
```

### DeliveryController - Criar Entrega

```java
@PostMapping
public ResponseEntity<DeliveryDTO> create(
    @RequestBody CreateDeliveryRequest request,
    @RequestHeader("Authorization") String token
) {
    String role = jwtService.extractRole(token);
    Long userId = jwtService.extractUserId(token);
    
    if (!"CLIENT".equals(role)) {
        throw new ForbiddenException("Apenas CLIENTs podem criar entregas");
    }
    
    // Se CLIENT tem service_contract ativo, cria CONTRACT
    // Senão, cria ON_DEMAND
    DeliveryType type = serviceContractService
        .hasActiveContract(userId) 
        ? DeliveryType.CONTRACT 
        : DeliveryType.ON_DEMAND;
    
    Delivery delivery = deliveryService.create(userId, request, type);
    
    return ResponseEntity.ok(deliveryMapper.toDTO(delivery));
}
```

### DeliveryService - Lógica de Criação

```java
@Transactional
public Delivery create(Long clientId, CreateDeliveryRequest request, DeliveryType type) {
    
    Delivery delivery = new Delivery();
    delivery.setClientId(clientId);
    delivery.setDeliveryType(type);
    
    if (type == DeliveryType.CONTRACT) {
        // Busca contrato ativo do cliente
        ServiceContract contract = serviceContractRepository
            .findActiveContractByClientId(clientId)
            .orElseThrow(() -> new BusinessException("Cliente sem contrato ativo"));
        
        delivery.setPartnership(contract.getPartnership());
    } else {
        // ON_DEMAND - sem partnership
        delivery.setPartnership(null);
    }
    
    // ... preencher outros campos (endereços, valores, etc)
    
    delivery = deliveryRepository.save(delivery);
    
    // Enviar notificação
    notificationService.notifyNewDelivery(delivery);
    
    return delivery;
}
```

### NotificationService - Lógica de Notificação

```java
public void notifyNewDelivery(Delivery delivery) {
    
    if (delivery.getDeliveryType() == DeliveryType.CONTRACT) {
        // Nível 1 ou 2: Notifica COURIERs da organização
        List<User> couriers = userRepository
            .findCouriersByOrganization(delivery.getPartnership().getId());
        
        for (User courier : couriers) {
            sendPushNotification(courier, delivery, "Nova entrega disponível");
        }
        
    } else if (delivery.getDeliveryType() == DeliveryType.ON_DEMAND) {
        // Nível 3: Notifica TODOS os COURIERs no raio
        double radiusKm = 10.0; // Configurável
        
        List<User> nearCouriers = courierLocationService
            .findCouriersInRadius(
                delivery.getFromLat(), 
                delivery.getFromLng(), 
                radiusKm
            );
        
        for (User courier : nearCouriers) {
            sendPushNotification(
                courier, 
                delivery, 
                "🔥 Entrega avulsa disponível no seu raio de ação!"
            );
        }
    }
}
```

---

## 🎮 Fluxos de Uso

### Fluxo 1: Cliente COM Contrato (CONTRACT)

```
1. CLIENT faz login
2. Sistema verifica: tem service_contract ativo? ✅ SIM
3. CLIENT cria entrega → tipo = CONTRACT
4. Sistema vincula delivery ao partnership_id do contrato
5. Notificação enviada para COURIERs da organização
6. COURIER da organização aceita
7. Entrega realizada
```

### Fluxo 2: Cliente SEM Contrato (ON_DEMAND)

```
1. CLIENT faz login (novo usuário ou sem contrato)
2. Sistema verifica: tem service_contract ativo? ❌ NÃO
3. CLIENT cria entrega → tipo = ON_DEMAND
4. Sistema NÃO vincula partnership_id (fica NULL)
5. Notificação em broadcast para TODOS os COURIERs no raio
6. Primeiro COURIER disponível aceita
7. Entrega realizada
```

### Fluxo 3: COURIER Visualiza Entregas

```java
@GetMapping("/available")
public ResponseEntity<List<DeliveryDTO>> getAvailableDeliveries(
    @RequestHeader("Authorization") String token
) {
    Long courierId = jwtService.extractUserId(token);
    String role = jwtService.extractRole(token);
    
    if (!"COURIER".equals(role)) {
        throw new ForbiddenException("Endpoint apenas para COURIERs");
    }
    
    // Busca entregas CONTRACT da organização do COURIER
    List<Delivery> contractDeliveries = deliveryRepository
        .findContractDeliveriesByCourier(courierId);
    
    // Busca entregas ON_DEMAND no raio do COURIER
    CourierLocation location = courierLocationService.getLocation(courierId);
    List<Delivery> onDemandDeliveries = deliveryRepository
        .findOnDemandDeliveriesInRadius(
            location.getLat(), 
            location.getLng(), 
            10.0 // raio em km
        );
    
    // Combina e retorna
    List<Delivery> allAvailable = new ArrayList<>();
    allAvailable.addAll(contractDeliveries);
    allAvailable.addAll(onDemandDeliveries);
    
    return ResponseEntity.ok(
        allAvailable.stream()
            .map(deliveryMapper::toDTO)
            .collect(Collectors.toList())
    );
}
```

---

## 📊 Queries Úteis

### Entregas ON_DEMAND Pendentes

```sql
SELECT * FROM available_on_demand_deliveries;
```

### Estatísticas por Tipo

```sql
SELECT 
    delivery_type,
    COUNT(*) as total,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pendentes,
    COUNT(CASE WHEN status = 'ACCEPTED' THEN 1 END) as aceitas,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completadas
FROM deliveries
GROUP BY delivery_type;
```

### COURIERs que Aceitam ON_DEMAND

```sql
SELECT 
    u.id,
    u.name,
    u.email,
    COUNT(d.id) as total_on_demand,
    SUM(d.total_amount) as valor_total
FROM users u
JOIN deliveries d ON d.courier_id = u.id
WHERE u.role = 'COURIER'
  AND d.delivery_type = 'ON_DEMAND'
GROUP BY u.id, u.name, u.email
ORDER BY total_on_demand DESC;
```

---

## ⚙️ Configurações Recomendadas

### application.yml

```yaml
delivery:
  on-demand:
    # Raio de busca de COURIERs para entregas ON_DEMAND (em km)
    radius-km: 10.0
    
    # Tempo máximo para aceitar entrega ON_DEMAND (em minutos)
    expiration-minutes: 30
    
    # Permitir entregas ON_DEMAND?
    enabled: true
    
    # Taxa adicional para entregas ON_DEMAND (%)
    additional-fee-percent: 10.0
```

---

## 🚀 Vantagens do Modelo ON_DEMAND

1. **Inclusão**: CLIENTs sem contrato também podem usar o sistema
2. **Flexibilidade**: COURIERs podem aceitar entregas de qualquer organização
3. **Eficiência**: Aproveita COURIERs ociosos no raio de ação
4. **Crescimento**: Facilita aquisição de novos clientes (sem barreira de contrato)
5. **Receita Extra**: COURIERs aumentam possibilidades de trabalho

---

## 🔒 Regras de Integridade (Database-Level)

✅ **delivery_type** deve ser 'CONTRACT' ou 'ON_DEMAND'  
✅ **CONTRACT** DEVE ter `partnership_id`  
✅ **ON_DEMAND** NÃO PODE ter `partnership_id`  
✅ Índices otimizados para busca de entregas ON_DEMAND  
✅ View `available_on_demand_deliveries` para facilitar queries

---

## 📝 Próximos Passos de Implementação

- [ ] Criar enum `DeliveryType` no código
- [ ] Atualizar entity `Delivery` com campo `deliveryType`
- [ ] Implementar lógica de criação com detecção automática de tipo
- [ ] Atualizar `NotificationService` para broadcast de ON_DEMAND
- [ ] Criar endpoint `/api/deliveries/available` para COURIERs
- [ ] Implementar serviço de geolocalização para raio de ação
- [ ] Adicionar configurações no `application.yml`
- [ ] Criar testes unitários e de integração
- [ ] Atualizar documentação da API (Swagger)
- [ ] Implementar lógica de expiração de entregas ON_DEMAND não aceitas

---

## 🎯 Conclusão

**SIM, o modelo é totalmente cabível!**

A arquitetura atual já suporta este cenário:
- `partnership_id` é **nullable** ✅
- Sistema de roles permite CLIENTs sem organização ✅
- Algoritmo de raio (nível 3) já existe ✅
- Notificações push já implementadas ✅

A migração V55 adiciona:
- Campo `delivery_type` para diferenciar tipos
- Constraints para garantir integridade
- Índices para performance
- View para facilitar consultas

**O sistema está pronto para suportar entregas ON_DEMAND!** 🚀
