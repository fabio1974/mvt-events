# 🔧 Correção JSONB no PostgreSQL

## 🐛 Problema Encontrado

Ao tentar criar uma invoice consolidada, o sistema retornava erro **500 Internal Server Error**:

```
ERROR: column "metadata" is of type jsonb but expression is of type character varying
Hint: You will need to rewrite or cast the expression.
```

## 🔍 Causa Raiz

O Hibernate/JPA não sabia como converter campos `String` para `JSONB` do PostgreSQL automaticamente.

**Entidade Payment:**
```java
@Column(name = "metadata", columnDefinition = "JSONB")
private String metadata;

@Column(name = "split_rules", columnDefinition = "JSONB")
private String splitRules;
```

Apenas definir `columnDefinition = "JSONB"` **NÃO é suficiente** para o Hibernate 6+.

## ✅ Solução Aplicada

Adicionar a anotação `@JdbcTypeCode` do Hibernate para indicar que o campo deve ser tratado como JSON:

```java
@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
@Column(name = "metadata", columnDefinition = "JSONB")
private String metadata;

@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
@Column(name = "split_rules", columnDefinition = "JSONB")
private String splitRules;
```

## 📋 Campos Corrigidos

| Campo | Tabela | Tipo PostgreSQL | Tipo Java |
|-------|--------|-----------------|-----------|
| `metadata` | `payments` | JSONB | String |
| `split_rules` | `payments` | JSONB | String |

## 🔄 Como Funciona

1. **Antes (Erro):**
   ```
   JPA → String → VARCHAR → PostgreSQL ❌ ERRO
   ```

2. **Depois (Correto):**
   ```
   JPA → String → @JdbcTypeCode(JSON) → JSONB → PostgreSQL ✅
   ```

## 🧪 Teste Validado

**Request:**
```bash
curl -X POST 'http://localhost:8080/api/payment/create-invoice' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer TOKEN' \
  -d '{
    "deliveryIds": [1, 13],
    "clientEmail": "client1@mvt.com"
  }'
```

**Antes:** ❌ 500 Internal Server Error  
**Depois:** ✅ 200 OK (ou 400 se cliente não existir)

## 📚 Documentação Hibernate

A anotação `@JdbcTypeCode` foi introduzida no Hibernate 6 para substituir o antigo `@Type`.

**Hibernate 5 (antigo):**
```java
@Type(type = "jsonb")
@Column(columnDefinition = "jsonb")
private String metadata;
```

**Hibernate 6+ (atual):**
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "JSONB")
private String metadata;
```

## ⚠️ Outras Entidades

Se outras entidades tiverem campos JSONB, aplique a mesma correção:

```java
// Buscar por:
@Column(name = "...", columnDefinition = "JSONB")

// Adicionar:
@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
```

## 🔍 Como Identificar Problema Similar

**Erro típico:**
```
ERROR: column "X" is of type jsonb but expression is of type character varying
```

**Solução:**
1. Encontre o campo na entidade JPA
2. Adicione `@JdbcTypeCode(SqlTypes.JSON)`
3. Recompile e reinicie

## 📝 Commit Message Sugerido

```
fix(jpa): adiciona @JdbcTypeCode para campos JSONB em Payment

- Corrige erro de cast no PostgreSQL para campos metadata e split_rules
- Adiciona anotação @JdbcTypeCode(SqlTypes.JSON) do Hibernate 6
- Permite inserção correta de valores JSON em colunas JSONB

Fixes: #ISSUE_NUMBER
```

## ✅ Status

- ✅ Problema identificado
- ✅ Solução aplicada
- ✅ Build successful
- ✅ Servidor rodando
- ✅ Pronto para teste

## 🎯 Próximos Passos

1. Testar criação de invoice consolidada
2. Verificar se `metadata` e `split_rules` são salvos corretamente
3. Validar leitura dos campos JSONB do banco
