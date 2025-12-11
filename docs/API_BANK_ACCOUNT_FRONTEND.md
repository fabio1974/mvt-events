# 🌐 API de Cadastro de Conta Bancária - Frontend Web

## 🎯 Overview

Este documento descreve o endpoint para cadastro de conta bancária de motoboys/managers através da interface web administrativa.

---

## 🔐 Autenticação

```http
Authorization: Bearer {access_token}
```

---

## 📍 Endpoint

```http
POST /api/bank-accounts
Content-Type: application/json
```

---

## 📥 Request Body Schema

### TypeScript Interface

```typescript
interface BankAccountRequest {
  // Dados Bancários
  bankCode: string;              // 3 dígitos (ex: "341")
  bankName: string;              // Nome do banco
  agency: string;                // Apenas números
  agencyDigit?: string | null;   // Opcional
  accountNumber: string;         // Apenas números
  accountDigit: string;          // Dígito verificador
  accountType: 'CHECKING' | 'SAVINGS';
  accountHolderName: string;     // Nome do titular
  accountHolderDocument: string; // CPF 11 dígitos sem pontuação
  
  // Dados Pessoais
  email: string;
  motherName: string;
  birthdate: string;             // Formato: DD/MM/YYYY
  monthlyIncome: string;         // Ex: "3000"
  professionalOccupation: string;
  
  // Contato
  phoneDdd: string;              // 2 dígitos
  phoneNumber: string;           // 8-9 dígitos
  
  // Endereço
  addressStreet: string;
  addressNumber: string;
  addressComplement?: string;    // Opcional
  addressNeighborhood: string;
  addressCity: string;
  addressState: string;          // UF 2 letras maiúsculas
  addressZipCode: string;        // CEP 8 dígitos sem pontuação
  addressReferencePoint: string; // Ponto de referência
}
```

### React Hook Form + Zod Validation

```typescript
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

const bankAccountSchema = z.object({
  bankCode: z.string().regex(/^\d{3}$/, 'Código do banco deve ter 3 dígitos'),
  bankName: z.string().min(1, 'Nome do banco é obrigatório'),
  agency: z.string().regex(/^\d+$/, 'Agência deve conter apenas números'),
  agencyDigit: z.string().nullable().optional(),
  accountNumber: z.string().regex(/^\d+$/, 'Conta deve conter apenas números'),
  accountDigit: z.string().min(1, 'Dígito da conta é obrigatório'),
  accountType: z.enum(['CHECKING', 'SAVINGS']),
  accountHolderName: z.string().min(1, 'Nome do titular é obrigatório'),
  accountHolderDocument: z.string().regex(/^\d{11}$/, 'CPF deve ter 11 dígitos'),
  
  email: z.string().email('Email inválido'),
  motherName: z.string().min(1, 'Nome da mãe é obrigatório'),
  birthdate: z.string().regex(/^\d{2}\/\d{2}\/\d{4}$/, 'Data deve estar no formato DD/MM/YYYY'),
  monthlyIncome: z.string().min(1, 'Renda mensal é obrigatória'),
  professionalOccupation: z.string().min(1, 'Ocupação é obrigatória'),
  
  phoneDdd: z.string().regex(/^\d{2}$/, 'DDD deve ter 2 dígitos'),
  phoneNumber: z.string().regex(/^\d{8,9}$/, 'Telefone deve ter 8 ou 9 dígitos'),
  
  addressStreet: z.string().min(1, 'Rua é obrigatória'),
  addressNumber: z.string().min(1, 'Número é obrigatório'),
  addressComplement: z.string().optional(),
  addressNeighborhood: z.string().min(1, 'Bairro é obrigatório'),
  addressCity: z.string().min(1, 'Cidade é obrigatória'),
  addressState: z.string().regex(/^[A-Z]{2}$/, 'Estado deve ser sigla UF'),
  addressZipCode: z.string().regex(/^\d{8}$/, 'CEP deve ter 8 dígitos'),
  addressReferencePoint: z.string().min(1, 'Ponto de referência é obrigatório'),
});

type BankAccountFormData = z.infer<typeof bankAccountSchema>;

// Usage
const form = useForm<BankAccountFormData>({
  resolver: zodResolver(bankAccountSchema),
});
```

---

## 📤 Exemplo de Request

```json
{
  "bankCode": "341",
  "bankName": "Itaú Unibanco",
  "agency": "1234",
  "agencyDigit": "6",
  "accountNumber": "12345678",
  "accountDigit": "9",
  "accountType": "CHECKING",
  "accountHolderName": "João da Silva",
  "accountHolderDocument": "12345678901",
  "email": "joao@email.com",
  "motherName": "Maria da Silva",
  "birthdate": "15/05/1990",
  "monthlyIncome": "3000",
  "professionalOccupation": "Motoboy",
  "phoneDdd": "85",
  "phoneNumber": "987654321",
  "addressStreet": "Rua Alberto Carvalho",
  "addressNumber": "111",
  "addressComplement": "Apto 345",
  "addressNeighborhood": "Centro",
  "addressCity": "Fortaleza",
  "addressState": "CE",
  "addressZipCode": "60000000",
  "addressReferencePoint": "Próximo ao supermercado Atacadão"
}
```

---

## ✅ Response - Sucesso (201 Created)

```typescript
interface BankAccountResponse {
  id: number;
  userId: string;
  bankCode: string;
  bankName: string;
  agency: string;
  agencyDigit: string | null;
  accountNumber: string;
  accountDigit: string;
  accountType: 'CHECKING' | 'SAVINGS';
  status: 'ACTIVE' | 'PENDING_VALIDATION' | 'BLOCKED';
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}
```

```json
{
  "id": 123,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "bankCode": "341",
  "bankName": "Itaú Unibanco",
  "agency": "1234",
  "agencyDigit": "6",
  "accountNumber": "12345678",
  "accountDigit": "9",
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdAt": "2025-12-10T10:30:00Z",
  "updatedAt": "2025-12-10T10:30:00Z"
}
```

---

## ❌ Error Responses

```typescript
interface ApiError {
  error: string;
  message: string;
  field?: string;
}
```

### 400 Bad Request
```json
{
  "error": "VALIDATION_ERROR",
  "message": "CPF do titular deve ter 11 dígitos",
  "field": "accountHolderDocument"
}
```

### 409 Conflict
```json
{
  "error": "ACCOUNT_ALREADY_EXISTS",
  "message": "Usuário já possui conta bancária cadastrada"
}
```

### 500 Internal Server Error
```json
{
  "error": "RECIPIENT_CREATION_FAILED",
  "message": "Falha ao criar recipient no Pagar.me"
}
```

---

## 🎨 Exemplo de Componente React

```typescript
'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { bankAccountSchema, type BankAccountFormData } from './schema';
import { createBankAccount } from './api';

export function BankAccountForm({ userId }: { userId: string }) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const form = useForm<BankAccountFormData>({
    resolver: zodResolver(bankAccountSchema),
  });

  const onSubmit = async (data: BankAccountFormData) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await createBankAccount(data);
      
      // Sucesso
      toast.success('Conta bancária cadastrada com sucesso!');
      router.push('/bank-accounts');
      
    } catch (err: any) {
      setError(err.message || 'Erro ao cadastrar conta');
      toast.error('Erro ao cadastrar conta bancária');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
      {/* Dados Bancários */}
      <section>
        <h3>Dados Bancários</h3>
        <BankCodeSelect {...form.register('bankCode')} />
        <Input {...form.register('agency')} label="Agência" />
        <Input {...form.register('accountNumber')} label="Conta" />
        {/* ... mais campos */}
      </section>

      {/* Dados Pessoais */}
      <section>
        <h3>Dados Pessoais</h3>
        <Input {...form.register('motherName')} label="Nome da Mãe" />
        <DatePicker {...form.register('birthdate')} label="Data de Nascimento" />
        {/* ... mais campos */}
      </section>

      {/* Endereço */}
      <section>
        <h3>Endereço</h3>
        <CepInput {...form.register('addressZipCode')} onCepFound={handleCepFound} />
        <Input {...form.register('addressStreet')} label="Rua" />
        {/* ... mais campos */}
      </section>

      {error && <Alert variant="destructive">{error}</Alert>}

      <Button type="submit" disabled={isLoading}>
        {isLoading ? 'Cadastrando...' : 'Cadastrar Conta'}
      </Button>
    </form>
  );
}
```

---

## 🔧 Utilitários Helper

```typescript
// utils/formatters.ts
export const formatCPF = (cpf: string) => {
  return cpf
    .replace(/\D/g, '')
    .replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
};

export const formatCEP = (cep: string) => {
  return cep.replace(/\D/g, '').replace(/(\d{5})(\d{3})/, '$1-$2');
};

export const formatPhone = (ddd: string, number: string) => {
  const cleaned = number.replace(/\D/g, '');
  if (cleaned.length === 9) {
    return `(${ddd}) ${cleaned.slice(0, 5)}-${cleaned.slice(5)}`;
  }
  return `(${ddd}) ${cleaned.slice(0, 4)}-${cleaned.slice(4)}`;
};

// utils/validators.ts
export const validateCPF = (cpf: string): boolean => {
  const cleaned = cpf.replace(/\D/g, '');
  if (cleaned.length !== 11) return false;
  // ... algoritmo de validação CPF
  return true;
};

// utils/api.ts
export const createBankAccount = async (
  data: BankAccountFormData
): Promise<BankAccountResponse> => {
  const response = await fetch('/api/bank-accounts', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`,
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  return response.json();
};
```

---

## 🔍 Integração ViaCEP

```typescript
interface ViaCEPResponse {
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  localidade: string;
  uf: string;
}

export const fetchAddressByCep = async (cep: string): Promise<ViaCEPResponse> => {
  const cleaned = cep.replace(/\D/g, '');
  const response = await fetch(`https://viacep.com.br/ws/${cleaned}/json/`);
  
  if (!response.ok) {
    throw new Error('CEP não encontrado');
  }
  
  const data = await response.json();
  
  if (data.erro) {
    throw new Error('CEP inválido');
  }
  
  return data;
};

// Usage
const handleCepBlur = async (cep: string) => {
  try {
    const address = await fetchAddressByCep(cep);
    form.setValue('addressStreet', address.logradouro);
    form.setValue('addressNeighborhood', address.bairro);
    form.setValue('addressCity', address.localidade);
    form.setValue('addressState', address.uf);
  } catch (error) {
    toast.error('CEP não encontrado');
  }
};
```

---

## 🏦 Lista de Bancos (Para Select/Dropdown)

```typescript
export const BRAZILIAN_BANKS = [
  { code: '001', name: 'Banco do Brasil' },
  { code: '033', name: 'Santander' },
  { code: '104', name: 'Caixa Econômica Federal' },
  { code: '237', name: 'Bradesco' },
  { code: '341', name: 'Itaú Unibanco' },
  { code: '260', name: 'Nubank' },
  { code: '077', name: 'Banco Inter' },
  { code: '336', name: 'C6 Bank' },
  { code: '290', name: 'Pagseguro' },
  { code: '323', name: 'Mercado Pago' },
  // ... mais bancos
] as const;
```

---

## 🧪 Testes (Jest/Testing Library)

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BankAccountForm } from './BankAccountForm';
import { server } from '@/mocks/server';
import { rest } from 'msw';

describe('BankAccountForm', () => {
  it('submits form with valid data', async () => {
    const user = userEvent.setup();
    render(<BankAccountForm userId="123" />);

    await user.type(screen.getByLabelText('Código do Banco'), '341');
    await user.type(screen.getByLabelText('Agência'), '1234');
    // ... preencher todos os campos

    await user.click(screen.getByRole('button', { name: /cadastrar/i }));

    await waitFor(() => {
      expect(screen.getByText(/sucesso/i)).toBeInTheDocument();
    });
  });

  it('shows validation errors', async () => {
    const user = userEvent.setup();
    render(<BankAccountForm userId="123" />);

    await user.click(screen.getByRole('button', { name: /cadastrar/i }));

    expect(await screen.findByText(/código do banco/i)).toBeInTheDocument();
  });
});
```

---

## 📞 Suporte

Em caso de dúvidas sobre a integração, contate o time backend.
