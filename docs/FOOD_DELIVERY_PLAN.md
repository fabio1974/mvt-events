# Zapi-Food — Plano de Expansão Food Delivery

## Visão Geral

Expandir o Zapi10 para suportar delivery de alimentos, farmácia, mercado e outros estabelecimentos.
Foco inicial: **Serra da Ibiapaba (CE)** — região turística com baixa penetração do iFood.

---

## 1. Modelo de Negócio

### Roles (sem mudança)

| Role | Quem é | Exemplo |
|---|---|---|
| **CLIENT** | Estabelecimento (PJ) | Restaurante, farmácia, lanchonete, pousada |
| **CUSTOMER** | Consumidor final (PF) | Quem pede comida pelo app |
| **COURIER** | Entregador | Motoboy |
| **ORGANIZER** | Intermediário/gerente | Recruta CLIENTs e COURIERs, ganha comissão |
| **ADMIN** | Administrador | Gestão global |

### Subrole do CLIENT (campo `service_type` em `users`)

| Valor | Descrição | Tem catálogo? |
|---|---|---|
| `RESTAURANT` | Restaurante/lanchonete | ✅ Sim |
| `PHARMACY` | Farmácia | ✅ Sim |
| `MARKET` | Mercado/mercearia | ✅ Sim |
| `LODGING` | Pousada/hotel | ✅ Sim |
| `GENERAL` | Entregas gerais (padrão atual) | ❌ Não |

### Fluxo de Dinheiro (Split Pagar.me)

```
CUSTOMER paga R$ 53,00
├── Comida (R$ 45,00)
│   ├── CLIENT (restaurante): 85-90%
│   └── ORGANIZER + Zapi10: 10-15% (comissão)
├── Taxa de entrega (R$ 8,00)
│   ├── COURIER: 85-87%
│   └── ORGANIZER + Zapi10: 13-15% (comissão)
```

Tudo via split automático do Pagar.me — já implementado para deliveries.

---

## 2. Novas Entidades

### ProductCategory (categorias do cardápio)

```sql
CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),   -- restaurante dono
    name VARCHAR(100) NOT NULL,                      -- "Pizzas", "Bebidas"
    description TEXT,
    display_order INT DEFAULT 0,                     -- ordem de exibição
    active BOOLEAN DEFAULT true,
    image_url VARCHAR(500),                          -- foto da categoria (opcional)
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### Product (prato/item do cardápio)

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),       -- restaurante dono
    category_id BIGINT REFERENCES product_categories(id),
    name VARCHAR(200) NOT NULL,                          -- "Pizza Margherita"
    description TEXT,                                     -- "Molho, mussarela, manjericão"
    price NUMERIC(10,2) NOT NULL,                        -- 29.90
    image_url VARCHAR(500),                              -- URL do Cloudinary
    available BOOLEAN DEFAULT true,                      -- pausar item
    preparation_time_minutes INT,                        -- tempo estimado de preparo
    display_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_products_client ON products(client_id);
CREATE INDEX idx_products_category ON products(category_id);
```

### Order (pedido do consumidor)

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(id),     -- quem pediu
    client_id UUID NOT NULL REFERENCES users(id),        -- restaurante
    delivery_id BIGINT REFERENCES deliveries(id),        -- criado quando "pronto"
    status VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    -- Status: PLACED → ACCEPTED → PREPARING → READY → DELIVERING → COMPLETED → CANCELLED
    subtotal NUMERIC(10,2) NOT NULL,                     -- valor dos itens
    delivery_fee NUMERIC(10,2),                          -- taxa de entrega
    total NUMERIC(10,2) NOT NULL,                        -- subtotal + delivery_fee
    notes TEXT,                                           -- "sem cebola", "interfone 302"
    estimated_preparation_minutes INT,                   -- soma dos tempos dos itens
    accepted_at TIMESTAMPTZ,
    preparing_at TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_client ON orders(client_id);
CREATE INDEX idx_orders_status ON orders(status);
```

### OrderItem (itens do pedido)

```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(10,2) NOT NULL,        -- snapshot do preço no momento
    notes TEXT,                                -- "borda recheada", "sem gelo"
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
```

---

## 3. Upload de Imagens — Cloudinary

### Por que Cloudinary

- **Grátis**: 25GB storage + 25GB bandwidth/mês
- **CDN global**: imagens servidas rápido
- **Transformação automática**: resize, crop, webp via URL
- **Volume estimado**: 50-100 restaurantes × 20 pratos = ~2.000 fotos = ~2GB

### Configuração no BE

Variáveis de ambiente:
```properties
CLOUDINARY_CLOUD_NAME=zapi10
CLOUDINARY_API_KEY=xxx
CLOUDINARY_API_SECRET=xxx
```

Dependência Maven:
```xml
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http5</artifactId>
    <version>2.0.0</version>
</dependency>
```

### API de Upload

```
POST /api/images/upload
Content-Type: multipart/form-data
Body: file (imagem), folder ("products" ou "categories")
Response: { "url": "https://res.cloudinary.com/zapi10/image/upload/v1/products/abc123.jpg" }
```

### Transformações automáticas via URL

```
Original:   /image/upload/products/pizza.jpg                           (3MB)
Thumbnail:  /image/upload/w_300,h_300,c_fill/products/pizza.jpg       (30KB)
Lista:      /image/upload/w_600,h_400,c_fill,q_80/products/pizza.jpg  (80KB)
Mobile:     /image/upload/w_800,q_75,f_auto/products/pizza.jpg        (120KB)
```

O BE armazena apenas a URL base. O frontend/mobile monta a URL com transformação conforme necessidade.

---

## 4. Fluxos

### 4.1 Restaurante cadastra cardápio

```
CLIENT (restaurante) abre app/web
  → "Meu Cardápio"
  → Cria categoria: "Pizzas" (nome, ordem)
  → Adiciona produto:
      1. Tira foto (câmera) ou escolhe da galeria
      2. App faz POST /api/images/upload → Cloudinary → retorna URL
      3. Preenche: nome, preço, descrição, tempo de preparo
      4. POST /api/products → salva com image_url
  → Pode pausar/ativar itens a qualquer momento
  → Pode abrir/fechar o estabelecimento (campo em users ou nova coluna)
```

### 4.2 Consumidor faz pedido

```
CUSTOMER abre app
  → Tela "Pedir" — lista restaurantes próximos
      - Filtros: tipo (restaurante, farmácia), distância, aberto agora
      - Cards: foto, nome, avaliação, tempo estimado, taxa de entrega
  → Toca num restaurante → vê cardápio por categoria
      - Cada item: foto, nome, preço, descrição
      - Botão "+" adiciona ao carrinho
  → Carrinho (bottom sheet ou tela):
      - Lista de itens com quantidade
      - Observações por item
      - Subtotal + taxa de entrega = total
      - Observação geral do pedido
  → "Finalizar Pedido" → pagamento (PIX ou cartão)
  → Order criada com status PLACED
  → Push para restaurante
```

### 4.3 Restaurante gerencia pedido

```
CLIENT (restaurante) recebe push "Novo pedido #123"
  → Abre tela de pedidos
  → Vê itens, observações, valor
  → "Aceitar" (PLACED → ACCEPTED)
  → Começa a preparar (ACCEPTED → PREPARING)
  → Marca "Pronto para retirada" (PREPARING → READY)
      → Sistema cria Delivery automaticamente (origem = restaurante, destino = customer)
      → Algoritmo de 3 níveis notifica couriers
  → Courier coleta → entrega
  → Order → COMPLETED
```

### 4.4 Restaurante rejeita pedido

```
CLIENT (restaurante) recebe push
  → "Rejeitar" → motivo (item indisponível, fechando, etc)
  → Order → CANCELLED
  → CUSTOMER recebe push + estorno automático
```

---

## 5. Telas Novas

### Mobile (mvt-mobile)

| Tela | Role | Descrição |
|---|---|---|
| Vitrine de restaurantes | CUSTOMER | Lista de CLIENTs tipo RESTAURANT próximos |
| Cardápio do restaurante | CUSTOMER | Produtos por categoria com fotos |
| Carrinho | CUSTOMER | Itens selecionados, qtd, observações, total |
| Checkout | CUSTOMER | Endereço de entrega, pagamento, confirmar |
| Meus Pedidos | CUSTOMER | Histórico e acompanhamento |
| Meu Cardápio | CLIENT | CRUD de categorias e produtos com upload de foto |
| Pedidos Recebidos | CLIENT | Lista de pedidos com ações (aceitar/preparar/pronto/rejeitar) |
| Abrir/Fechar loja | CLIENT | Toggle de disponibilidade |

### Web (mvt-fe)

| Tela | Role | Descrição |
|---|---|---|
| Gestão de Cardápio | CLIENT | CRUD completo de categorias e produtos |
| Painel de Pedidos | CLIENT | Lista de pedidos em tempo real |
| Relatórios | CLIENT/ADMIN | Vendas, itens mais pedidos, horários de pico |

---

## 6. Endpoints Novos (BE)

### Imagens
```
POST   /api/images/upload              — Upload para Cloudinary (multipart)
DELETE /api/images/{publicId}           — Remove do Cloudinary
```

### Categorias
```
GET    /api/clients/{clientId}/categories        — Listar categorias
POST   /api/clients/{clientId}/categories        — Criar categoria
PUT    /api/clients/{clientId}/categories/{id}    — Editar
DELETE /api/clients/{clientId}/categories/{id}    — Remover
```

### Produtos
```
GET    /api/clients/{clientId}/products           — Listar produtos (com filtros)
POST   /api/clients/{clientId}/products           — Criar produto
PUT    /api/clients/{clientId}/products/{id}       — Editar
DELETE /api/clients/{clientId}/products/{id}       — Remover
PATCH  /api/clients/{clientId}/products/{id}/toggle — Pausar/ativar
```

### Vitrine (público para CUSTOMER)
```
GET    /api/stores                                — Listar restaurantes próximos
GET    /api/stores/{clientId}/menu                — Cardápio completo (categorias + produtos)
```

### Pedidos
```
POST   /api/orders                                — Criar pedido (CUSTOMER)
GET    /api/orders                                — Listar pedidos (filtrado por role)
GET    /api/orders/{id}                           — Detalhe do pedido
PATCH  /api/orders/{id}/accept                    — Aceitar (CLIENT)
PATCH  /api/orders/{id}/preparing                 — Iniciou preparo (CLIENT)
PATCH  /api/orders/{id}/ready                     — Pronto para retirada (CLIENT) → cria Delivery
PATCH  /api/orders/{id}/cancel                    — Cancelar (CLIENT ou CUSTOMER)
```

---

## 7. Perfil da Loja (tabela separada)

Tabela `store_profiles` — relação 1:1 com `users` (apenas CLIENTs com catálogo):

```sql
CREATE TABLE store_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    is_open BOOLEAN DEFAULT false,
    opening_hours JSONB,
    -- Exemplo: {"seg": ["11:00-14:00", "18:00-22:00"], "dom": ["11:00-15:00"]}
    min_order NUMERIC(10,2),
    avg_preparation_minutes INT,
    logo_url VARCHAR(500),
    cover_url VARCHAR(500),
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_store_profiles_user ON store_profiles(user_id);
CREATE INDEX idx_store_profiles_open ON store_profiles(is_open) WHERE is_open = true;
```

Vantagens:
- Tabela `users` não cresce (já tem 27 colunas)
- Apenas CLIENTs tipo RESTAURANT/PHARMACY/etc têm store_profile
- COURIER, CUSTOMER, ADMIN não são afetados

---

## 8. Sequência de Implementação

### Fase 1 — Infraestrutura (1-2 dias)
1. Cloudinary: conta, config, ImageService, endpoint de upload
2. Migrations: product_categories, products, orders, order_items
3. Entidades JPA: ProductCategory, Product, Order, OrderItem

### Fase 2 — Cardápio do Restaurante (2-3 dias)
4. API: CRUD de categorias e produtos
5. Mobile: tela "Meu Cardápio" (CLIENT) com upload de foto
6. Web: gestão de cardápio no FE (EntityCRUD ou tela custom)

### Fase 3 — Vitrine e Pedido (3-4 dias)
7. API: endpoints de vitrine (stores, menu) e pedidos
8. Mobile: tela vitrine de restaurantes (CUSTOMER)
9. Mobile: tela cardápio + carrinho + checkout
10. Integração: Order → Delivery (quando restaurante marca "pronto")

### Fase 4 — Painel do Restaurante (2 dias)
11. Mobile: tela "Pedidos Recebidos" (CLIENT) com ações
12. Push notifications: novo pedido, pedido pronto, etc.

### Fase 5 — Polimento (1-2 dias)
13. Horário de funcionamento, loja aberta/fechada
14. Pedido mínimo
15. Avaliação do restaurante
16. Testes e ajustes

**Total estimado: 10-13 dias de desenvolvimento**

---

## 9. O que NÃO muda

- ✅ Delivery flow (PENDING → ACCEPTED → IN_TRANSIT → COMPLETED)
- ✅ Algoritmo de 3 níveis para notificar couriers
- ✅ Contratos (service_contracts, employment_contracts)
- ✅ Pagamentos (Pagar.me split)
- ✅ Rastreamento (tracking link via WhatsApp)
- ✅ Push notifications (FCM)
- ✅ Roles existentes (CLIENT, CUSTOMER, COURIER, ORGANIZER, ADMIN)
- ✅ Multi-tenancy e segurança
