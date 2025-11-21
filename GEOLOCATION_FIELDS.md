# Campos de Geolocalização na Entidade User

## Visão Geral

A entidade `User` possui **4 campos de localização** divididos em duas categorias:

### 1. Coordenadas do Endereço Fixo (latitude/longitude)
- **Campos:** `latitude` e `longitude`
- **Colunas no banco:** `latitude` e `longitude`
- **Propósito:** Armazenar a localização fixa do endereço do usuário (residência, negócio, etc.)
- **Atualização:** Via endpoint PUT `/api/users/{id}` (atualização geral do perfil)
- **Uso:** Identificar onde o usuário/cliente está baseado

### 2. Coordenadas GPS em Tempo Real (gpsLatitude/gpsLongitude)
- **Campos:** `gpsLatitude` e `gpsLongitude`
- **Colunas no banco:** `gps_latitude` e `gps_longitude`
- **Propósito:** Tracking em tempo real da localização atual do usuário (especialmente COURIER)
- **Atualização:** Via endpoint PUT `/api/users/{id}/location` (atualização específica de localização)
- **Uso:** Rastreamento de entregadores, cálculo de proximidade, etc.

---

## Endpoints da API

### 1. Atualizar Perfil Completo (inclui endereço fixo)
```bash
PUT /api/users/{id}
Content-Type: application/json

{
  "name": "João Silva",
  "phone": "85999999999",
  "address": "Rua Exemplo, 123",
  "latitude": -3.7327,    // Coordenadas do endereço
  "longitude": -38.5270,
  "cpf": "12345678909",
  ...
}
```

**Resposta:**
```json
{
  "id": "uuid",
  "name": "João Silva",
  "address": "Rua Exemplo, 123",
  "latitude": -3.7327,       // Endereço fixo
  "longitude": -38.5270,
  "gpsLatitude": -3.7400,    // Última localização GPS (pode ser diferente)
  "gpsLongitude": -38.5350,
  "updatedAt": "2025-11-21T10:30:00",
  ...
}
```

### 2. Atualizar Localização GPS (tracking em tempo real)
```bash
PUT /api/users/{id}/location
Content-Type: application/json

{
  "latitude": -3.7400,    // Coordenadas GPS atuais
  "longitude": -38.5350,
  "updatedAt": "2025-11-21T10:30:00"  // Timestamp do GPS (opcional)
}
```

**Resposta:**
```json
{
  "id": "uuid",
  "name": "João Silva",
  "latitude": -3.7327,       // Endereço fixo (inalterado)
  "longitude": -38.5270,
  "gpsLatitude": -3.7400,    // Localização GPS atualizada
  "gpsLongitude": -38.5350,
  "updatedAt": "2025-11-21T10:30:00",
  ...
}
```

---

## Casos de Uso

### Cliente (CLIENT)
- **latitude/longitude:** Coordenadas do endereço de entrega padrão
- **gpsLatitude/gpsLongitude:** Geralmente não usado (cliente não se move)

### Entregador (COURIER)
- **latitude/longitude:** Coordenadas da base/residência do entregador
- **gpsLatitude/gpsLongitude:** Posição atual do entregador (atualizada constantemente pelo app)

### Administrador (ADMIN/ORGANIZER)
- **latitude/longitude:** Coordenadas do escritório/sede
- **gpsLatitude/gpsLongitude:** Opcional

---

## Migrações

- **V62:** Renomeou `latitude`/`longitude` para `gps_latitude`/`gps_longitude`
- **V63:** Renomeou `client_lat`/`client_lng` para `latitude`/`longitude`

## Traduções (JpaMetadataExtractor)

```java
FIELD_TRANSLATIONS.put("latitude", "Latitude");
FIELD_TRANSLATIONS.put("longitude", "Longitude");
FIELD_TRANSLATIONS.put("gpsLatitude", "Latitude GPS");
FIELD_TRANSLATIONS.put("gpsLongitude", "Longitude GPS");
```

---

## JWT Token

### Campos incluídos no token JWT após login:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "name": "João Silva",
  "role": "COURIER",
  "address": "Rua Exemplo, 123",
  "latitude": -3.7327,     // Coordenadas do endereço fixo
  "longitude": -38.5270,
  "phone": "85999999999",
  "cpf": "123.456.789-09",
  ...
}
```

**Observação:** O token JWT contém apenas as coordenadas do **endereço fixo** (`latitude`/`longitude`). As coordenadas GPS em tempo real (`gpsLatitude`/`gpsLongitude`) **não** estão no token, pois mudam frequentemente e devem ser obtidas através de requisições à API.

### Métodos disponíveis no JwtUtil:

```java
// Extrair coordenadas do endereço do token
Double latitude = jwtUtil.getLatitudeFromToken(token);
Double longitude = jwtUtil.getLongitudeFromToken(token);

// Obter todos os dados do usuário (incluindo lat/lng)
Map<String, Object> userData = jwtUtil.getUserDataFromToken(token);
Double lat = (Double) userData.get("latitude");
Double lng = (Double) userData.get("longitude");
```

---

## Frontend - Boas Práticas

### Formulário de Cadastro/Edição de Perfil
```javascript
// Usar latitude/longitude para endereço fixo
const profileData = {
  address: "Rua Exemplo, 123",
  latitude: -3.7327,
  longitude: -38.5270
};
```

### Tracking de Entregador em Tempo Real
```javascript
// Enviar gpsLatitude/gpsLongitude a cada X segundos
navigator.geolocation.watchPosition((position) => {
  fetch(`/api/users/${userId}/location`, {
    method: 'PUT',
    body: JSON.stringify({
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
      updatedAt: new Date().toISOString()
    })
  });
});
```

### Exibição no Mapa
```javascript
// Mostrar endereço fixo
const addressMarker = {
  lat: user.latitude,
  lng: user.longitude,
  type: 'address',
  icon: 'home'
};

// Mostrar posição atual do entregador
const currentMarker = {
  lat: user.gpsLatitude,
  lng: user.gpsLongitude,
  type: 'gps',
  icon: 'motorcycle'
};
```
