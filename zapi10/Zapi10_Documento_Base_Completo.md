# 📘 Zapi10 – Documento Base Completo

## 🏍️ 1. Visão Geral
O **Zapi10** é um aplicativo de **entregas e mototaxistas** criado para oferecer **taxas justas, inclusão produtiva e autonomia aos trabalhadores locais**, em parceria com **prefeituras e pequenos empreendedores**.  
A proposta é modernizar a logística urbana e fortalecer a economia local, oferecendo uma alternativa sustentável aos grandes aplicativos do mercado.

## 🎯 2. Objetivos do Projeto
- Reduzir a dependência de grandes plataformas que cobram taxas abusivas.  
- Formalizar mototaxistas e entregadores informais, oferecendo **seguro, cobertura previdenciária e capacitação**.  
- Promover **integração institucional com prefeituras**, fortalecendo políticas de emprego e mobilidade.  
- Gerar **renda local** através de um modelo descentralizado e colaborativo.

## 🧩 3. Estrutura Operacional
O modelo do Zapi10 é composto por três camadas:

| Nível                 | Responsável                              | Função                                              |
| --------------------- | ---------------------------------------- | --------------------------------------------------- |
| **Plataforma Zapi10** | Núcleo de desenvolvimento e suporte      | Gestão de tecnologia, marketing e pagamentos        |
| **ADMs regionais**    | Representantes locais / Parceiros Zapi10 | Cadastram e gerenciam grupos de motoboys            |
| **Motoboys**          | Profissionais independentes              | Realizam as entregas e recebem diretamente pelo app |

Os **ADMs** funcionam como pequenos gestores autônomos de grupos, sendo remunerados por comissão.

## 💰 4. Modelo de Negócio

### Distribuição de Receita (exemplo-base)
- **Custo médio por entrega:** R$ 7,00  
- **Entregas/dia por motoboy:** 20  
- **Motoboys:** 200  
- **Dias/mês:** 30  

💵 **Faturamento total:** R$ 7 × 20 × 200 × 30 = **R$ 840.000,00/mês**

**Divisão:**
- Motoboy: 85% → R$ 714.000  
- ADMs (5 grupos): 5% → R$ 42.000 (R$ 8.400 cada)  
- Plataforma Zapi10: 10% → R$ 84.000  

### Cenários Variáveis
Simulações também feitas com:
- Entregas de R$ 7 a R$ 15  
- Volume de 20 a 25 entregas/dia  
- Taxas ajustáveis conforme região e tipo de serviço

## 🧠 5. Estratégia de Implantação
1. **Fase 1 – Piloto Local:** Iniciar em **Ubajara/CE**, com 1 grupo (20 motoboys).  
2. **Fase 2 – Expansão Regional:** Estender para municípios da **Serra da Ibiapaba**.  
3. **Fase 3 – Parcerias Institucionais:** Formalizar acordos com prefeituras, associações e cooperativas locais.  
4. **Fase 4 – Integração com comércios locais:** Restaurantes, farmácias, mercados e lojas passam a usar o app para entregas.  

## 🏛️ 6. Parcerias com Prefeituras
O Zapi10 propõe **cooperação institucional** para promover a inclusão produtiva:

- Criação de **cadastro municipal de mototaxistas e entregadores**.  
- Apoio à **formalização via MEI e cobertura previdenciária (INSS e DPVAT)**.  
- Parceria em **programas de capacitação e segurança viária**.  
- Utilização do Zapi10 em **entregas públicas** (remédios, documentos, vacinas, etc.).

Essas parcerias fortalecem a legitimidade do projeto e o impacto social local.

## 🧱 7. Arquitetura Técnica Inicial
**Stack sugerida:**
- **Frontend:** React (PWA, com Capacitor futuramente para app nativo)  
- **Backend:** Spring Boot ou Node (Nest.js)  
- **Banco:** PostgreSQL  
- **Autenticação:** JWT (integração futura com gov.br)  
- **Pagamentos:** PIX + gateway (PagSeguro, MercadoPago, ou próprio)  
- **Mapas:** Google Maps API ou OpenStreetMap  
- **Infraestrutura:** Docker + Render / Railway  

### Principais Entidades
- **Usuário:** cliente, motoboy, ADM ou administrador da plataforma  
- **Entrega:** origem, destino, valor, status, timestamps  
- **Grupo:** ADM responsável, motoboys associados  
- **Pagamento:** valor total, repasse ADM e plataforma  
- **Parceria Municipal:** dados da prefeitura, convênios, seguros  
- **Avaliação:** feedback entre usuários  

## 🎨 8. Identidade Visual
- **Nome:** Zapi10  
- **Símbolo:** moto em movimento com traço de velocidade  
- **Cores:** laranja (energia), azul (confiança), branco (transparência)  
- **Conceito:** agilidade, proximidade e inclusão.  

## 📊 9. Projeções Futuras
- Criação do **Zapi10 Marketplace**, com entregas e mototáxi integrados.  
- Ranking de desempenho e **sistema de gamificação**.  
- Expansão estadual, priorizando **economias locais e microempreendedores**.  
- Lançamento de **versão white label** para prefeituras.  

## ✅ 10. Próximos Passos
1. Refinar modelo de dados (ERD) e fluxos principais (cadastro, pedido, pagamento).  
2. Criar protótipo funcional no **Figma**.  
3. Implementar backend mínimo com endpoints REST.  
4. Definir piloto local (Ubajara).  
5. Redigir minuta de **acordo de cooperação com a prefeitura**.  
