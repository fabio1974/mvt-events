# API - Endpoint de Bancos Brasileiros

## 📋 Visão Geral

Endpoint público para listar todos os bancos brasileiros disponíveis no sistema. Útil para construir selects/dropdowns de bancos no mobile.

---

## 🔗 Endpoint

```
GET /api/banks
```

**Base URL**: `https://seu-servidor.com` (ou `http://localhost:8080` em desenvolvimento)

**Autenticação**: ❌ Não requer autenticação (endpoint público)

---

## 📤 Request

### Headers
```
Content-Type: application/json
```

### Parâmetros
Nenhum parâmetro necessário.

---

## 📥 Response

### Status: `200 OK`

### Body (JSON Array)
```json
[
  {
    "code": "001",
    "name": "Banco do Brasil"
  },
  {
    "code": "033",
    "name": "Banco Santander"
  },
  {
    "code": "104",
    "name": "Caixa Econômica Federal"
  },
  {
    "code": "237",
    "name": "Banco Bradesco"
  },
  {
    "code": "341",
    "name": "Banco Itaú"
  },
  {
    "code": "260",
    "name": "Nubank (Nu Pagamentos)"
  },
  {
    "code": "077",
    "name": "Banco Inter"
  },
  {
    "code": "290",
    "name": "PagSeguro (PagBank)"
  },
  {
    "code": "323",
    "name": "Mercado Pago"
  },
  {
    "code": "380",
    "name": "PicPay"
  }
]
```

### Campos do Response

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `code` | String | Código BACEN do banco (3 dígitos). Ex: "001", "260", "341" |
| `name` | String | Nome completo do banco. Ex: "Banco do Brasil", "Nubank (Nu Pagamentos)" |

---

## 💻 Exemplo de Uso no Mobile

### React Native / JavaScript
```javascript
const fetchBanks = async () => {
  try {
    const response = await fetch('https://seu-servidor.com/api/banks', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    const banks = await response.json();
    
    // Usar no select/dropdown
    setBankList(banks);
    
    console.log('Total de bancos:', banks.length);
    // Exemplo: Total de bancos: 49
    
  } catch (error) {
    console.error('Erro ao buscar bancos:', error);
  }
};
```

### Flutter / Dart
```dart
Future<List<Bank>> fetchBanks() async {
  try {
    final response = await http.get(
      Uri.parse('https://seu-servidor.com/api/banks'),
      headers: {'Content-Type': 'application/json'},
    );
    
    if (response.statusCode == 200) {
      List<dynamic> jsonList = json.decode(response.body);
      return jsonList.map((json) => Bank.fromJson(json)).toList();
    } else {
      throw Exception('Falha ao carregar bancos');
    }
  } catch (e) {
    print('Erro: $e');
    rethrow;
  }
}

class Bank {
  final String code;
  final String name;
  
  Bank({required this.code, required this.name});
  
  factory Bank.fromJson(Map<String, dynamic> json) {
    return Bank(
      code: json['code'],
      name: json['name'],
    );
  }
}
```

### Kotlin (Android)
```kotlin
data class Bank(
    val code: String,
    val name: String
)

suspend fun fetchBanks(): List<Bank> {
    val response = client.get("https://seu-servidor.com/api/banks") {
        contentType(ContentType.Application.Json)
    }
    return response.body()
}
```

### Swift (iOS)
```swift
struct Bank: Codable {
    let code: String
    let name: String
}

func fetchBanks() async throws -> [Bank] {
    let url = URL(string: "https://seu-servidor.com/api/banks")!
    let (data, _) = try await URLSession.shared.data(from: url)
    return try JSONDecoder().decode([Bank].self, from: data)
}
```

---

## 🎨 Exemplo de Select/Dropdown

### React Native
```javascript
import { Picker } from '@react-native-picker/picker';

function BankSelector() {
  const [banks, setBanks] = useState([]);
  const [selectedBank, setSelectedBank] = useState('');

  useEffect(() => {
    fetchBanks().then(setBanks);
  }, []);

  return (
    <Picker
      selectedValue={selectedBank}
      onValueChange={(itemValue) => setSelectedBank(itemValue)}
    >
      <Picker.Item label="Selecione um banco" value="" />
      {banks.map((bank) => (
        <Picker.Item 
          key={bank.code} 
          label={`${bank.code} - ${bank.name}`} 
          value={bank.code} 
        />
      ))}
    </Picker>
  );
}
```

### Flutter
```dart
DropdownButton<String>(
  hint: Text('Selecione um banco'),
  value: selectedBankCode,
  items: banks.map((bank) {
    return DropdownMenuItem<String>(
      value: bank.code,
      child: Text('${bank.code} - ${bank.name}'),
    );
  }).toList(),
  onChanged: (value) {
    setState(() {
      selectedBankCode = value;
    });
  },
)
```

---

## 📌 Informações Importantes

### ✅ Vantagens
- **Sem autenticação**: Endpoint público, não precisa de token
- **Centralizado**: Lista mantida no backend, atualizações automáticas
- **Completo**: Retorna 49 bancos brasileiros (tradicionais + digitais)
- **Validado**: Usa mesma lista das validações do sistema

### 🔄 Cache Recomendado
Como a lista de bancos muda raramente, recomenda-se:
- Cachear a resposta no app (AsyncStorage, SharedPreferences, etc.)
- Revalidar a cada 7 dias ou a cada update do app
- Buscar do servidor apenas se cache estiver vazio/expirado

### 🏦 Bancos Inclusos (principais)
- Tradicionais: BB, Santander, Caixa, Bradesco, Itaú, Safra
- Digitais: Nubank, Inter, PagSeguro, Mercado Pago, PicPay, C6, Original
- Investimentos: BTG Pactual, XP, Modal
- Cooperativas: Sicredi, Bancoob, Unicred, Ailos

### 📝 Quando Usar o `code`
O campo `code` deve ser usado para:
- Enviar ao backend ao salvar dados bancários
- Armazenar no banco de dados local
- Fazer validações de conta bancária

### 🎨 Quando Usar o `name`
O campo `name` deve ser usado para:
- Exibir na interface do usuário
- Mostrar no select/dropdown
- Facilitar busca por nome

---

## 🧪 Testando

### cURL
```bash
curl -X GET "https://seu-servidor.com/api/banks" \
  -H "Content-Type: application/json"
```

### Postman
1. Método: `GET`
2. URL: `https://seu-servidor.com/api/banks`
3. Headers: `Content-Type: application/json`
4. Sem Body
5. Send

---

## 📊 Total de Bancos
Atualmente: **49 bancos**

Lista completa ordenada e atualizada conforme cadastro do Banco Central (BACEN).

---

## 🆘 Suporte

Em caso de dúvidas ou problemas:
- Verificar se a URL base está correta
- Endpoint não requer autenticação
- Response sempre retorna array (mesmo que vazio em caso de erro)

---

**Última atualização**: 07/01/2026
