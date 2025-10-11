# 🎯 Exemplo Prático - React + TypeScript

## Event Form Component - Completo

```typescript
// types/Event.ts
export interface Event {
  id?: number;
  name: string;
  slug: string;
  description?: string;
  eventType: EventType;
  eventDate: string;
  city?: { id: number };
  location: string;
  maxParticipants?: number;
  registrationOpen?: boolean;
  registrationStartDate?: string;
  registrationEndDate?: string;
  price?: number;
  currency?: string;
  status?: EventStatus;
  transferFrequency?: TransferFrequency;
  organization: { id: number };
  platformFeePercentage?: number;
  termsAndConditions?: string;
}

export enum EventType {
  RUNNING = "RUNNING",
  CYCLING = "CYCLING",
  MARATHON = "MARATHON",
  // ...
}

export enum EventStatus {
  DRAFT = "DRAFT",
  PUBLISHED = "PUBLISHED",
  CANCELLED = "CANCELLED",
  COMPLETED = "COMPLETED",
}

export interface City {
  id: number;
  name: string;
  state: string;
  stateCode: string;
}
```

```typescript
// components/EventForm.tsx
import React, { useState, useEffect } from "react";
import { Event, City, EventType, EventStatus } from "../types/Event";

interface EventFormProps {
  eventId?: number;
  organizationId: number;
  onSuccess?: (event: Event) => void;
}

export function EventForm({
  eventId,
  organizationId,
  onSuccess,
}: EventFormProps) {
  const [loading, setLoading] = useState(false);
  const [metadata, setMetadata] = useState<any>(null);
  const [cities, setCities] = useState<City[]>([]);
  const [formData, setFormData] = useState({
    name: "",
    slug: "",
    description: "",
    eventType: "RUNNING" as EventType,
    eventDate: "",
    cityId: null as number | null,
    cityName: "",
    location: "",
    maxParticipants: "",
    registrationOpen: true,
    registrationStartDate: "",
    registrationEndDate: "",
    price: "",
    currency: "BRL",
    status: "DRAFT" as EventStatus,
    transferFrequency: "WEEKLY",
    platformFeePercentage: "5",
    termsAndConditions: "",
  });

  // 1. Carregar metadados
  useEffect(() => {
    fetch("/api/metadata/event")
      .then((r) => r.json())
      .then(setMetadata);
  }, []);

  // 2. Carregar evento existente (se editando)
  useEffect(() => {
    if (!eventId) return;

    setLoading(true);
    fetch(`/api/events/${eventId}`)
      .then((r) => r.json())
      .then((event) => {
        setFormData({
          name: event.name,
          slug: event.slug,
          description: event.description || "",
          eventType: event.eventType,
          eventDate: event.eventDate?.substring(0, 16) || "", // YYYY-MM-DDTHH:mm
          cityId: event.city?.id || null,
          cityName: event.city?.name || "",
          location: event.location,
          maxParticipants: event.maxParticipants?.toString() || "",
          registrationOpen: event.registrationOpen ?? true,
          registrationStartDate: event.registrationStartDate || "",
          registrationEndDate: event.registrationEndDate || "",
          price: event.price?.toString() || "",
          currency: event.currency || "BRL",
          status: event.status || "DRAFT",
          transferFrequency: event.transferFrequency || "WEEKLY",
          platformFeePercentage: event.platformFeePercentage?.toString() || "5",
          termsAndConditions: event.termsAndConditions || "",
        });
      })
      .finally(() => setLoading(false));
  }, [eventId]);

  // 3. Autocomplete de cidades
  const searchCities = async (query: string) => {
    if (query.length < 2) {
      setCities([]);
      return;
    }

    const results = await fetch(`/api/cities/search?q=${query}`).then((r) =>
      r.json()
    );

    setCities(results);
  };

  // 4. Handle de mudanças no form
  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >
  ) => {
    const { name, value, type } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]:
        type === "checkbox" ? (e.target as HTMLInputElement).checked : value,
    }));

    // Auto-gerar slug do nome
    if (name === "name" && !eventId) {
      const slug = value
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "");

      setFormData((prev) => ({ ...prev, slug }));
    }
  };

  // 5. Salvar evento
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Montar payload genérico
    const payload: Event = {
      name: formData.name,
      slug: formData.slug,
      description: formData.description || undefined,
      eventType: formData.eventType,
      eventDate: formData.eventDate,
      location: formData.location,
      maxParticipants: formData.maxParticipants
        ? parseInt(formData.maxParticipants)
        : undefined,
      registrationOpen: formData.registrationOpen,
      registrationStartDate: formData.registrationStartDate || undefined,
      registrationEndDate: formData.registrationEndDate || undefined,
      price: formData.price ? parseFloat(formData.price) : undefined,
      currency: formData.currency,
      status: formData.status,
      transferFrequency: formData.transferFrequency as any,
      platformFeePercentage: formData.platformFeePercentage
        ? parseFloat(formData.platformFeePercentage)
        : undefined,
      termsAndConditions: formData.termsAndConditions || undefined,

      // Relacionamentos como objetos com ID
      organization: { id: organizationId },
      city: formData.cityId ? { id: formData.cityId } : undefined,
    };

    // Adicionar ID se editando
    if (eventId) {
      payload.id = eventId;
    }

    setLoading(true);

    try {
      const response = await fetch(
        eventId ? `/api/events/${eventId}` : "/api/events",
        {
          method: eventId ? "PUT" : "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
          body: JSON.stringify(payload),
        }
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || "Erro ao salvar evento");
      }

      const savedEvent = await response.json();

      alert(
        eventId
          ? "Evento atualizado com sucesso!"
          : "Evento criado com sucesso!"
      );

      if (onSuccess) {
        onSuccess(savedEvent);
      }
    } catch (error) {
      alert(error.message);
    } finally {
      setLoading(false);
    }
  };

  if (!metadata) {
    return <div>Carregando metadados...</div>;
  }

  return (
    <form onSubmit={handleSubmit} className="event-form">
      <h2>{eventId ? "Editar Evento" : "Novo Evento"}</h2>

      {/* Nome */}
      <div className="form-group">
        <label htmlFor="name">Nome *</label>
        <input
          type="text"
          id="name"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
          maxLength={100}
        />
      </div>

      {/* Slug */}
      <div className="form-group">
        <label htmlFor="slug">Slug *</label>
        <input
          type="text"
          id="slug"
          name="slug"
          value={formData.slug}
          onChange={handleChange}
          required
          pattern="[a-z0-9-]+"
        />
      </div>

      {/* Tipo de Evento */}
      <div className="form-group">
        <label htmlFor="eventType">Tipo de Evento *</label>
        <select
          id="eventType"
          name="eventType"
          value={formData.eventType}
          onChange={handleChange}
          required
        >
          {metadata.formFields
            .find((f: any) => f.name === "eventType")
            ?.options?.map((opt: any) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
        </select>
      </div>

      {/* Data do Evento */}
      <div className="form-group">
        <label htmlFor="eventDate">Data do Evento *</label>
        <input
          type="datetime-local"
          id="eventDate"
          name="eventDate"
          value={formData.eventDate}
          onChange={handleChange}
          required
        />
      </div>

      {/* Cidade - Autocomplete */}
      <div className="form-group">
        <label htmlFor="citySearch">Cidade *</label>
        <input
          type="text"
          id="citySearch"
          value={formData.cityName}
          onChange={(e) => {
            setFormData((prev) => ({ ...prev, cityName: e.target.value }));
            searchCities(e.target.value);
          }}
          placeholder="Digite para buscar..."
          required
        />
        {cities.length > 0 && (
          <ul className="autocomplete-list">
            {cities.map((city) => (
              <li
                key={city.id}
                onClick={() => {
                  setFormData((prev) => ({
                    ...prev,
                    cityId: city.id,
                    cityName: `${city.name} - ${city.stateCode}`,
                  }));
                  setCities([]);
                }}
              >
                {city.name} - {city.stateCode}
              </li>
            ))}
          </ul>
        )}
        {formData.cityId && (
          <small className="text-muted">
            Cidade selecionada: ID {formData.cityId}
          </small>
        )}
      </div>

      {/* Local */}
      <div className="form-group">
        <label htmlFor="location">Local *</label>
        <input
          type="text"
          id="location"
          name="location"
          value={formData.location}
          onChange={handleChange}
          required
          maxLength={150}
        />
      </div>

      {/* Máximo de Participantes */}
      <div className="form-group">
        <label htmlFor="maxParticipants">Máximo de Participantes</label>
        <input
          type="number"
          id="maxParticipants"
          name="maxParticipants"
          value={formData.maxParticipants}
          onChange={handleChange}
          min="1"
          max="1000"
        />
      </div>

      {/* Inscrições Abertas */}
      <div className="form-group">
        <label>
          <input
            type="checkbox"
            name="registrationOpen"
            checked={formData.registrationOpen}
            onChange={handleChange}
          />
          Inscrições Abertas
        </label>
      </div>

      {/* Período de Inscrições */}
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="registrationStartDate">Início das Inscrições</label>
          <input
            type="date"
            id="registrationStartDate"
            name="registrationStartDate"
            value={formData.registrationStartDate}
            onChange={handleChange}
          />
        </div>
        <div className="form-group">
          <label htmlFor="registrationEndDate">Fim das Inscrições</label>
          <input
            type="date"
            id="registrationEndDate"
            name="registrationEndDate"
            value={formData.registrationEndDate}
            onChange={handleChange}
          />
        </div>
      </div>

      {/* Preço */}
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="price">Preço</label>
          <input
            type="number"
            id="price"
            name="price"
            value={formData.price}
            onChange={handleChange}
            step="0.01"
            min="0"
          />
        </div>
        <div className="form-group">
          <label htmlFor="currency">Moeda</label>
          <input
            type="text"
            id="currency"
            name="currency"
            value={formData.currency}
            onChange={handleChange}
            maxLength={3}
          />
        </div>
      </div>

      {/* Status */}
      <div className="form-group">
        <label htmlFor="status">Status</label>
        <select
          id="status"
          name="status"
          value={formData.status}
          onChange={handleChange}
        >
          {metadata.formFields
            .find((f: any) => f.name === "status")
            ?.options?.map((opt: any) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
        </select>
      </div>

      {/* Frequência de Transferência */}
      <div className="form-group">
        <label htmlFor="transferFrequency">Frequência de Transferência</label>
        <select
          id="transferFrequency"
          name="transferFrequency"
          value={formData.transferFrequency}
          onChange={handleChange}
        >
          {metadata.formFields
            .find((f: any) => f.name === "transferFrequency")
            ?.options?.map((opt: any) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
        </select>
      </div>

      {/* Descrição */}
      <div className="form-group">
        <label htmlFor="description">Descrição</label>
        <textarea
          id="description"
          name="description"
          value={formData.description}
          onChange={handleChange}
          rows={4}
          maxLength={1000}
        />
      </div>

      {/* Termos e Condições */}
      <div className="form-group">
        <label htmlFor="termsAndConditions">Termos e Condições</label>
        <textarea
          id="termsAndConditions"
          name="termsAndConditions"
          value={formData.termsAndConditions}
          onChange={handleChange}
          rows={4}
        />
      </div>

      {/* Botões */}
      <div className="form-actions">
        <button type="submit" disabled={loading || !formData.cityId}>
          {loading ? "Salvando..." : eventId ? "Atualizar" : "Criar"}
        </button>
        <button type="button" onClick={() => window.history.back()}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
```

---

## CSS Sugerido

```css
/* styles/EventForm.css */
.event-form {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.autocomplete-list {
  list-style: none;
  padding: 0;
  margin: 5px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.autocomplete-list li {
  padding: 10px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
}

.autocomplete-list li:hover {
  background-color: #f5f5f5;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 30px;
}

.form-actions button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.form-actions button[type="submit"] {
  background-color: #007bff;
  color: white;
}

.form-actions button[type="submit"]:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.form-actions button[type="button"] {
  background-color: #6c757d;
  color: white;
}

.text-muted {
  color: #6c757d;
  font-size: 12px;
}
```

---

## Uso do Componente

```typescript
// pages/EventEditPage.tsx
import { EventForm } from "../components/EventForm";
import { useParams, useNavigate } from "react-router-dom";

export function EventEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const organizationId = 6; // Pegar do contexto do usuário logado

  return (
    <div>
      <EventForm
        eventId={id ? parseInt(id) : undefined}
        organizationId={organizationId}
        onSuccess={(event) => {
          navigate(`/events/${event.id}`);
        }}
      />
    </div>
  );
}
```

---

## 🎯 Resultado Final

![Event Form](https://via.placeholder.com/800x600?text=Event+Form+Example)

Este formulário:

- ✅ Carrega metadados da API automaticamente
- ✅ Usa autocomplete para cidades
- ✅ Gera slug automaticamente do nome
- ✅ Valida campos obrigatórios
- ✅ Formata payload corretamente com `{city: {id}}` e `{organization: {id}}`
- ✅ Funciona para criar E editar eventos
- ✅ Mostra labels traduzidos dos ENUMs
- ✅ Type-safe com TypeScript

---

**Pronto para copiar e colar!** 🚀
