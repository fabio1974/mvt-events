# 📘 Zapi10 — ER Model (MER) – **English**

This document describes Zapi10’s **high‑level ER model**, layered and with **cardinalities**, ready for the **Obsidian Canvas**.

---

## 🧭 Domain Layers
- **User/Access:** User, Client, Courier, LocalManager (ADM).
- **Operational:** Delivery, Evaluation.
- **Financial:** Payment, Unified Payout, Payout Item.
- **Institutional:** Municipal Partnership, Client↔Manager Link (N:M).

---

## 🧩 Entities (high level)

### User
Base identity for all profiles.
- `id`, `name`, `tax_id`, `email`, `phone`, `type` (`CLIENT`, `COURIER`, `ADM`, `ADMIN`), `active`, `created_at`

### Client (subtype of User)
Requests deliveries and pays the platform.
- May link to **zero or many LocalManagers** via N:M.

### Courier (subtype of User)
Performs deliveries; receives payouts.

### LocalManager / ADM (subtype of User)
Manages a group of couriers; receives commissions and periodic payouts.

### Delivery
Core operational record.
- `client_id`, `courier_id`, `adm_id`, `from`, `to`, `total_value`, `status`

### Payment
1:1 with Delivery; splits amounts to courier, ADM, platform.
- Feeds **period closings** (Unified Payout).

### Unified Payout
Aggregates payments per period for a **beneficiary** (ADM or Courier).
- `type` (`ADM` | `COURIER`), `beneficiary_id`, `period`, `total_value`, `status`

### Payout Item
References the set of payments included in a payout.
- `payout_id`, `payment_id`, `item_value`

### Evaluation
Post‑delivery feedback (rating/comment).

### Municipal Partnership
Public agreements; can originate deliveries or link ADMs.

### ClientManagerLink (join table N:M)
Optional association between Client and ADM (0..N ↔ 0..N).

---

## 🔗 Cardinalities (summary)
- **Client 1:N Delivery** (creates)  
- **Courier 1:N Delivery** (performs)  
- **ADM 1:N Courier** (manages)  
- **Delivery 1:1 Payment** (generates)  
- **Unified Payout 1:N Payout Item**; **Payout Item N:1 Payment**  
- **Client N:M ADM** (via **ClientManagerLink**)  
- **Municipal Partnership 1:N ADM** | **1:N Delivery**  
- **Delivery 1:N Evaluation**

---

## 🗺️ Mermaid Diagram (horizontal)

> For a horizontal layout in Canvas, we use **flowchart LR** with edge labels showing cardinalities.

```mermaid
flowchart LR
  subgraph U["👥 User Layer"]
    User[(User)]
    Client[(Client)]
    Courier[(Courier)]
    ADM[(LocalManager/ADM)]
  end
  subgraph O["🧩 Operational Layer"]
    Delivery[(Delivery)]
    Evaluation[(Evaluation)]
  end
  subgraph F["💰 Financial Layer"]
    Payment[(Payment)]
    Payout[(Unified Payout)]
    PayoutItem[(Payout Item)]
  end
  subgraph I["🏛️ Institutional Layer"]
    Partnership[(Municipal Partnership)]
    Link[(ClientManagerLink)]
    Platform[(Platform - Zapi10)]
  end

  %% Specializations
  Client -- "is a" --> User
  Courier -- "is a" --> User
  ADM -- "is a" --> User

  %% Operational
  Client -- "1:N creates" --> Delivery
  Courier -- "1:N performs" --> Delivery
  ADM -- "1:N manages" --> Courier
  Delivery -- "1:N receives" --> Evaluation

  %% Financial
  Delivery -- "1:1 generates" --> Payment
  Client -- "1:N pays" --> Payment
  Payment -- "N:1 consolidated by" --> PayoutItem
  PayoutItem -- "N:1 belongs to" --> Payout
  Payout -- "pays" --> Courier
  Payout -- "pays" --> ADM
  Payment -- "revenue" --> Platform

  %% Institutional
  Partnership -- "1:N links" --> ADM
  Partnership -- "1:N originates" --> Delivery

  %% N:M Client <-> ADM
  Client --- "0..N" --- Link
  Link --- "0..N" --> ADM
```

---

## ✅ Implementation notes
- PostgreSQL: `payout(type, beneficiary_id, period)` + `payout_item(payout_id, payment_id, item_value)`.
- Integrity: `payment.total_value = courier_value + adm_value + platform_value`.
- `client_manager_link(client_id, adm_id)` with **unique** to prevent duplicates.
- Optional: `delivery_event` table for full delivery status history.
