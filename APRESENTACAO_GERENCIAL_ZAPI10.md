# 📊 Zapi10 - Apresentação Gerencial

**Plataforma de Entregas Inteligente**  
**Versão:** 1.0 - Novembro 2024  
**Público:** Gerentes e Gestores  
**Status:** Em Desenvolvimento

---

## 📑 Sumário Executivo

O **Zapi10** é uma plataforma de entregas que conecta clientes, grupos de logística e motoboys através de um sistema inteligente de notificações em 3 níveis, priorizando relacionamentos comerciais e garantindo cobertura total.

### 🎯 Principais Diferenciais

- ✅ **Sistema de Prioridades**: Grupos principais têm primeira chance
- ✅ **Cobertura Garantida**: 3 níveis de escalação automática
- ✅ **Flexibilidade**: Múltiplos contratos simultâneos
- ✅ **Transparência**: Divisão justa (85% motoboy / 15% grupo)

---

## 1️⃣ Como Funcionam os Grupos

### Vínculos de Clientes

Um cliente pode estar conectado a **vários grupos** simultaneamente:

```
CLIENTE: Restaurante Bella Italia
├── LogiFast Entregas ⭐ (Grupo Principal)
├── Expresso Rápido (Grupo Secundário)
└── MegaLog (Grupo Secundário)
```

**Regras:**
- ✅ Múltiplos grupos permitidos
- ✅ Apenas 1 grupo principal por cliente
- ✅ Grupos podem ser ativados/suspensos
- ✅ Histórico mantido permanentemente

### Vínculos de Motoboys

Um motoboy pode trabalhar para **vários grupos** ao mesmo tempo:

```
MOTOBOY: João Silva
├── LogiFast Entregas ✅ (Ativo)
├── Expresso Rápido ✅ (Ativo)
└── MegaLog ❌ (Inativo - histórico)
```

**Regras:**
- ✅ Trabalhar em múltiplos grupos
- ✅ Ativar/desativar sem perder histórico
- ✅ Recebe de todos os grupos ativos
- ✅ Sem obrigação de exclusividade

---

## 2️⃣ Sistema de Notificações em 3 Níveis

### 🥇 Nível 1 - Grupo Principal (Imediato)

**Quem recebe:** Apenas motoboys do grupo principal do cliente  
**Quando:** Imediatamente após solicitação  
**Raio:** 5 km (expande para 10 km se necessário)  
**Tempo de espera:** 2 minutos

**Exemplo:**
```
Cliente solicita → Sistema notifica 3 motoboys do grupo principal
→ Aguarda 2 minutos para aceite
```

**Vantagem:** Grupo principal tem **primeira chance** em todas as entregas

---

### 🥈 Nível 2 - Grupos Secundários (+2 min)

**Quem recebe:** Motoboys de TODOS os grupos secundários ativos  
**Quando:** Após 2 minutos sem aceite no Nível 1  
**Raio:** 5 km (expande para 10 km se necessário)  
**Tempo de espera:** Mais 2 minutos

**Exemplo:**
```
Nível 1 sem aceite → Sistema busca grupos secundários
→ Notifica motoboys de 2-3 grupos diferentes
→ Aguarda mais 2 minutos
```

**Vantagem:** Grupos secundários têm **oportunidade** de atender

---

### 🥉 Nível 3 - Todos Próximos (+4 min)

**Quem recebe:** QUALQUER motoboy disponível próximo  
**Quando:** Após 4 minutos sem aceite  
**Raio:** 5 km → 10 km  
**Restrição:** NENHUMA (sem filtro de grupo)

**Exemplo:**
```
Nível 2 sem aceite → Sistema remove restrições
→ Notifica TODOS os motoboys próximos
→ Inclusive grupos SEM contrato com cliente
```

**Vantagem:** **Garantia** de que a entrega será atendida

---

## 3️⃣ Divisão de Valores

### 💰 Modelo de Comissão

```
┌─────────────────────────────┐
│ VALOR DA ENTREGA: R$ 20,00 │
└─────────────────────────────┘
         │
    ┌────┴─────┐
    ▼          ▼
┌─────────┐ ┌────────┐
│MOTOBOY  │ │ GRUPO  │
│  85%    │ │  15%   │
│R$ 17,00 │ │ R$ 3,00│
└─────────┘ └────────┘
```

### 📊 Tabela de Referência

| Valor da Entrega | Motoboy (85%) | Grupo (15%) |
|------------------|---------------|-------------|
| R$ 10,00 | R$ 8,50 | R$ 1,50 |
| R$ 20,00 | R$ 17,00 | R$ 3,00 |
| R$ 30,00 | R$ 25,50 | R$ 4,50 |
| R$ 50,00 | R$ 42,50 | R$ 7,50 |
| R$ 100,00 | R$ 85,00 | R$ 15,00 |

---

## 4️⃣ Impacto Financeiro para Grupos

### Simulação Mensal

**Cliente:** Faz 100 entregas/mês (ticket médio R$ 20,00)

```
GRUPO PRINCIPAL (aceita 70% no Nível 1):
70 entregas × R$ 3,00 = R$ 210,00/mês ⭐

GRUPO SECUNDÁRIO (aceita 20% no Nível 2):
20 entregas × R$ 3,00 = R$ 60,00/mês

SEM CONTRATO (aceita 10% no Nível 3):
10 entregas × R$ 3,00 = R$ 30,00/mês
```

### 💡 Análise

**Ser grupo principal vale 3,5x mais que ser secundário!**

Estratégias para maximizar receita:
1. ✅ Tornar-se grupo principal de mais clientes
2. ✅ Manter motoboys sempre disponíveis
3. ✅ Garantir resposta rápida (aceitar em < 1 min)
4. ✅ Posicionar motoboys em regiões estratégicas

---

## 5️⃣ Exemplo Prático Completo

### 📍 Situação

```
Cliente: Restaurante Bella Italia
Local: Av. Paulista, São Paulo
Valor: R$ 25,00
Horário: 19h30 (movimento normal)
```

### ⏰ Linha do Tempo

**00:00 - Cliente solicita entrega**
```
Sistema identifica: LogiFast = Grupo Principal
Notifica: João (2.3km) e Maria (4.1km)
Status: Aguardando... ⏳
```

**02:00 - Ninguém aceitou, Nível 2 ativa**
```
Sistema busca: Grupos secundários (Expresso Rápido, MegaLog)
Notifica: Carlos (3.8km) e Bruno (7.2km)
Status: Aguardando... ⏳
```

**04:00 - Ainda sem aceite, Nível 3 ativa**
```
Sistema remove restrições
Notifica: Paula (8.3km) de outro grupo SEM contrato
Status: Aguardando... ⏳
```

**04:15 - ACEITE! 🎉**
```
Paula aceita a entrega

Resultado:
├── Paula ganha: R$ 21,25 (85%)
├── Grupo dela ganha: R$ 3,75 (15%)
└── Tempo total: 4min15s
```

### 📊 Análise do Caso

- ❌ LogiFast (grupo principal) perdeu R$ 3,75
- ❌ Expresso Rápido (secundário) perdeu R$ 3,75
- ✅ Grupo da Paula ganhou sem ter contrato!
- ✅ Cliente teve entrega atendida

**Lição:** Manter motoboys disponíveis é crucial!

---

## 6️⃣ Indicadores de Performance (KPIs)

### 📈 Metas Ideais

| Indicador | Meta | Interpretação |
|-----------|------|---------------|
| **Taxa Aceite Nível 1** | > 70% | Grupo principal está eficiente |
| **Taxa Aceite Nível 2** | 20-25% | Normal ter algumas no Nível 2 |
| **Taxa Aceite Nível 3** | < 10% | Pouco uso do Nível 3 é bom |
| **Tempo Médio Aceite** | < 1 min | Resposta rápida dos motoboys |
| **Taxa Não-Aceite** | < 3% | Cobertura está adequada |

### 📊 Exemplo de Relatório Mensal

```
═══════════════════════════════════════════
  GRUPO: LogiFast Entregas
  PERÍODO: Novembro/2024
═══════════════════════════════════════════

ENTREGAS TOTAIS: 1.250

Distribuição:
├─ Nível 1: 900 (72%) ✅ Excelente
├─ Nível 2: 280 (22%) ⚠️  Poderia ser menos
├─ Nível 3:  60 ( 5%) ✅ Ótimo
└─ Perdidas:  10 ( 1%) ✅ Excelente

Tempo Médio de Aceite: 58 segundos ✅

RECEITA GERADA:
├─ Nível 1: R$ 2.700,00 (900 × R$ 3,00)
├─ Nível 2: R$ 0,00 (atendido por outros)
├─ Nível 3: R$ 0,00 (atendido por outros)
└─ TOTAL: R$ 2.700,00

OPORTUNIDADES PERDIDAS:
- 280 entregas (Nível 2) = R$ 840,00 perdidos
- Potencial total: R$ 3.540,00
- Taxa de aproveitamento: 76%
```

---

## 7️⃣ Perguntas Frequentes

### Sobre Contratos

**❓ Posso ser secundário e depois virar principal?**  
✅ Sim! Estratégia comum: demonstrar qualidade no Nível 2, negociar upgrade.

**❓ Cliente pode trocar o grupo principal?**  
✅ Sim, a qualquer momento. Risco para o grupo atual!

**❓ Posso suspender um cliente inadimplente?**  
✅ Sim. Ele não verá seus motoboys mas outros grupos continuam atendendo.

### Sobre o Algoritmo

**❓ 2 minutos entre níveis não é muito?**  
⚖️ É o equilíbrio ideal entre dar chance ao grupo principal e não perder entregas.

**❓ E se dois motoboys aceitarem ao mesmo tempo?**  
🥇 O primeiro que clicar leva. O segundo recebe "Já foi aceita".

**❓ Posso configurar o raio de busca?**  
🔜 Não ainda, mas está no roadmap permitir customização por grupo.

### Sobre Comissões

**❓ A divisão 85/15 pode mudar?**  
📍 Atualmente é fixa, mas pode haver negociação futura por volume.

**❓ Quando recebo meus 15%?**  
💳 Automaticamente após conclusão e confirmação da entrega.

**❓ Cobro algo do cliente?**  
❌ Não. O valor já vem definido pelo cliente. Você recebe 15% dele.

---

## 8️⃣ Estratégias para Aumentar Receita

### 🎯 Para Grupos

**1. Seja Grupo Principal de Mais Clientes**
- Negocie upgrade de clientes secundários
- Ofereça SLA menor que concorrentes
- Demonstre qualidade no atendimento

**2. Mantenha Alta Disponibilidade**
- Recrute mais motoboys em regiões estratégicas
- Incentive motoboys a ficarem online
- Crie turnos para cobrir horários de pico

**3. Treine para Rapidez**
- Meta: aceitar em < 30 segundos
- 2 minutos é pouco - quem responde primeiro ganha
- Bonificação para aceites rápidos

**4. Posicionamento Inteligente**
- Analise onde seus clientes mais pedem
- Posicione motoboys próximos aos clientes principais
- Monitore raio de 5km dos pontos quentes

### 💡 ROI de Virar Grupo Principal

**Cenário:** Cliente faz 150 entregas/mês (R$ 22 cada)

```
COMO SECUNDÁRIO:
18% × 150 = 27 entregas/mês
27 × R$ 3,30 = R$ 89,10/mês
R$ 89,10 × 12 = R$ 1.069,20/ano

COMO PRINCIPAL:
72% × 150 = 108 entregas/mês
108 × R$ 3,30 = R$ 356,40/mês
R$ 356,40 × 12 = R$ 4.276,80/ano

GANHO: R$ 3.207,60/ano 🎯
```

**Vale a pena investir em conquistar o cliente!**

---

## 9️⃣ Casos de Uso Especiais

### 🟢 Caso 1: Cliente Novo

```
Cliente sem grupo principal definido

Comportamento:
└─ Nível 3 ativado IMEDIATAMENTE (T=0)
   └─ Todos os motoboys próximos notificados
   
Vantagem: Cliente novo não espera 4 minutos
```

### 🟡 Caso 2: Horário de Pico

```
Sexta-feira 20h - todos os motoboys ocupados

Nível 1: 0 disponíveis ❌
Nível 2: 0 disponíveis ❌
Nível 3: Encontra motoboy de grupo sem contrato ✅

Resultado: Entrega não é perdida
```

### 🔴 Caso 3: Região com Pouca Cobertura

```
Bairro afastado - poucos motoboys

Nível 1: Nenhum em 5km, encontra em 10km ✅
         
Importância do raio estendido:
- Garante cobertura em áreas remotas
- Evita perda de entregas
```

---

## 🔟 Roadmap e Melhorias Futuras

### 📅 Próximas Funcionalidades

**Fase 1 - Inteligência (Q1/2025)**
- 🤖 Algoritmo aprende qual motoboy aceita mais rápido
- 📊 Previsão de demanda por horário/região
- 🎯 Sugestão de onde posicionar motoboys

**Fase 2 - Personalização (Q2/2025)**
- ⚙️ Grupos podem configurar raio de busca
- ⏰ Tempo entre níveis customizável
- 💰 Negociação de comissão por volume

**Fase 3 - Gamificação (Q3/2025)**
- 🏆 Ranking de motoboys por performance
- 🎖️ Badges por conquistas
- 💎 Bônus por metas mensais

**Fase 4 - Analytics Avançado (Q4/2025)**
- 📈 Dashboard com métricas em tempo real
- 📉 Análise preditiva de churn
- 💡 Sugestões de otimização automáticas

---

## 📞 Próximos Passos

### Para Começar no Zapi10

1. **Cadastro do Grupo**
   - Dados da empresa (CNPJ, endereço, etc)
   - Definir gerente responsável
   - Configurações iniciais

2. **Cadastro de Motoboys**
   - Vincular motoboys ao grupo
   - Ativar contratos de trabalho
   - Verificar documentação

3. **Captar Clientes**
   - Oferecer como grupo principal
   - Ou começar como secundário
   - Demonstrar qualidade do serviço

4. **Monitorar Performance**
   - Acompanhar KPIs mensais
   - Ajustar estratégias
   - Otimizar disponibilidade

---

## 📊 Resumo Executivo

### ✨ Por que o Zapi10 é diferente?

**Para Clientes:**
- ✅ Nunca ficam sem atendimento (3 níveis)
- ✅ Sempre encontra motoboy próximo
- ✅ Flexibilidade de múltiplos grupos

**Para Motoboys:**
- ✅ Trabalhar para vários grupos
- ✅ Autonomia para escolher entregas
- ✅ 85% do valor (maior que maioria dos apps)

**Para Grupos:**
- ✅ Grupos principais têm vantagem competitiva
- ✅ Não precisa de motoboys exclusivos
- ✅ Pode atender clientes de outros grupos
- ✅ Sistema justo e transparente

### 🎯 Oportunidade

O mercado de entregas cresce **30% ao ano**. O Zapi10 oferece:
- Sistema mais justo que concorrentes
- Flexibilidade única no mercado
- Tecnologia de ponta
- Modelo sustentável para todos

---

**Fim da Apresentação Gerencial**

*Zapi10 - Plataforma de Entregas Inteligente*  
*Versão 1.0 - Novembro 2024*  
*Status: Em Desenvolvimento Ativo*

📧 Para mais informações, entre em contato com a equipe de desenvolvimento.
