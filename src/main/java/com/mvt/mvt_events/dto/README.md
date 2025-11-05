# DTOs (Data Transfer Objects)

## 📁 Estrutura de Pastas

```
dto/
├── BaseDTO.java                 # Classe base para todos os DTOs
├── common/                      # DTOs reutilizáveis para objetos aninhados
│   ├── CityDTO.java            # DTO para City (id, name, state)
│   └── OrganizationDTO.java    # DTO para Organization (id, name)
└── mapper/
    └── DTOMapper.java          # Utilitário para mapeamento Entity -> DTO
```

## 🎯 Objetivo

Centralizar todos os DTOs do projeto em um único lugar, facilitando:

- Reutilização de código
- Manutenção consistente
- Evitar duplicação de DTOs
- Padronizar respostas de API

## 🔧 Como Usar

### 1. DTOs Compartilhados (`dto/common/`)

Use estes DTOs quando precisar retornar objetos relacionados nas respostas:

```java
// No seu Response DTO
@Data
@NoArgsConstructor
public static class MyEntityResponse {
    private Long id;
    private String name;
    private CityDTO city;              // Use o DTO compartilhado
    private OrganizationDTO organization; // Use o DTO compartilhado

    public MyEntityResponse(MyEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();

        // Use DTOMapper para converter
        this.city = DTOMapper.toDTO(entity.getCity());
        this.organization = DTOMapper.toDTO(entity.getOrganization());
    }
}
```

### 2. DTOMapper

Centralize a lógica de conversão usando `DTOMapper`:

```java
// Converter City -> CityDTO
CityDTO cityDTO = DTOMapper.toDTO(entity.getCity());

// Converter Organization -> OrganizationDTO
OrganizationDTO orgDTO = DTOMapper.toDTO(entity.getOrganization());
```

**Vantagens:**

- Lógica de conversão em um único lugar
- Null-safe (retorna null se a entidade for null)
- Fácil de testar e manter

## 📝 Adicionar um Novo DTO Compartilhado

### Passo 1: Criar a classe DTO

Crie o arquivo em `dto/common/`:

```java
package com.mvt.mvt_events.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
}
```

### Passo 2: Adicionar método no DTOMapper

Edite `DTOMapper.java`:

```java
public static CategoryDTO toDTO(Category category) {
    if (category == null) {
        return null;
    }
    return new CategoryDTO(
        category.getId(),
        category.getName(),
        category.getDescription()
    );
}
```

### Passo 3: Usar no Controller

```java
import com.mvt.mvt_events.dto.common.CategoryDTO;
import com.mvt.mvt_events.dto.mapper.DTOMapper;

@Data
@NoArgsConstructor
public static class EventResponse {
    private Long id;
    private String title;
    private CategoryDTO category; // Objeto aninhado

    public EventResponse(Event event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.category = DTOMapper.toDTO(event.getCategory());
    }
}
```

## 📋 DTOs Disponíveis

### CityDTO

```java
{
  "id": 1068,
  "name": "Ubajara",
  "state": "Ceará"
}
```

**Quando usar:** Sempre que uma entidade tiver relacionamento com `City`

### OrganizationDTO

```java
{
  "id": 6,
  "name": "Moveltrack Sistemas"
}
```

**Quando usar:** Sempre que uma entidade tiver relacionamento com `Organization`

## ✅ Boas Práticas

1. **Sempre use DTOMapper** - Não crie instâncias manualmente
2. **DTOs são imutáveis** - Use `@AllArgsConstructor` e `@NoArgsConstructor`
3. **Minimalismo** - Inclua apenas campos essenciais nos DTOs compartilhados
4. **Documentação** - Adicione JavaDoc nos métodos do DTOMapper
5. **Null-safe** - DTOMapper sempre verifica null antes de converter

## 🔄 Fluxo de Dados

```
┌─────────────┐       ┌─────────────┐       ┌──────────────┐
│  Entity     │  -->  │  DTOMapper  │  -->  │  DTO         │
│  (JPA)      │       │  .toDTO()   │       │  (Response)  │
└─────────────┘       └─────────────┘       └──────────────┘
     ▼                                              ▼
 Database                                      JSON API
```

## 📚 Exemplos de Uso

Veja implementações completas em:

- `UserController.UserResponse` - Usa CityDTO e OrganizationDTO
- `OrganizationController.OrganizationResponse` - Usa CityDTO

## 🚫 Anti-Patterns (Evite!)

❌ **Criar DTOs duplicados em cada Controller:**

```java
// NÃO FAÇA ISSO!
public static class CityDTO {
    private Long id;
    private String name;
    private String state;
}
```

✅ **Use o DTO compartilhado:**

```java
// FAÇA ISSO!
import com.mvt.mvt_events.dto.common.CityDTO;
import com.mvt.mvt_events.dto.mapper.DTOMapper;

this.city = DTOMapper.toDTO(entity.getCity());
```

---

**Criado em:** Outubro 2025  
**Padrão estabelecido:** Controller Response Pattern  
**Documentação completa:** `/docs/implementation/CONTROLLER_RESPONSE_PATTERN.md`
