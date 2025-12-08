package com.mvt.mvt_events.util;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Constantes com códigos e nomes dos principais bancos brasileiros.
 * Códigos padronizados pelo Banco Central do Brasil (BACEN).
 * 
 * Referência: https://www.bcb.gov.br/pom/spb/estatistica/port/ASTR003.pdf
 */
@Component
public class BrazilianBanks {

    /**
     * Mapa com código → nome dos principais bancos brasileiros
     */
    private static final Map<String, String> BANKS;

    static {
        Map<String, String> banks = new LinkedHashMap<>();
        
        // ==================== BANCOS TRADICIONAIS ====================
        banks.put("001", "Banco do Brasil");
        banks.put("033", "Banco Santander");
        banks.put("104", "Caixa Econômica Federal");
        banks.put("237", "Banco Bradesco");
        banks.put("341", "Banco Itaú");
        banks.put("745", "Banco Citibank");
        banks.put("399", "HSBC Bank Brasil");
        banks.put("422", "Banco Safra");
        banks.put("389", "Banco Mercantil do Brasil");
        banks.put("756", "Banco Cooperativo do Brasil (Bancoob)");
        banks.put("748", "Banco Cooperativo Sicredi");
        
        // ==================== BANCOS DIGITAIS / FINTECHS ====================
        banks.put("260", "Nubank (Nu Pagamentos)");
        banks.put("077", "Banco Inter");
        banks.put("290", "PagSeguro (PagBank)");
        banks.put("323", "Mercado Pago");
        banks.put("380", "PicPay");
        banks.put("403", "Cora Sociedade de Crédito Direto");
        banks.put("197", "Stone Pagamentos");
        banks.put("084", "Uniprime Norte do Paraná");
        banks.put("329", "QI Sociedade de Crédito Direto");
        banks.put("364", "Gerencianet Pagamentos do Brasil");
        banks.put("102", "XP Investimentos");
        banks.put("348", "Banco XP");
        banks.put("654", "Banco Digimais (Banco Rendimento)");
        banks.put("655", "Banco Votorantim");
        banks.put("136", "Unicred Cooperativa");
        
        // ==================== BANCOS DE INVESTIMENTO ====================
        banks.put("208", "Banco BTG Pactual");
        banks.put("069", "Banco Crefisa");
        banks.put("021", "Banco Banestes");
        banks.put("047", "Banco do Estado de Sergipe (Banese)");
        banks.put("041", "Banco do Estado do Rio Grande do Sul (Banrisul)");
        banks.put("070", "Banco de Brasília (BRB)");
        banks.put("085", "Cooperativa Central de Crédito (Ailos)");
        
        // ==================== OUTROS BANCOS RELEVANTES ====================
        banks.put("212", "Banco Original");
        banks.put("336", "Banco C6");
        banks.put("422", "Banco Safra");
        banks.put("652", "Itaú Unibanco Holding");
        banks.put("623", "Banco PAN");
        banks.put("612", "Banco Guanabara");
        banks.put("604", "Banco Industrial do Brasil");
        banks.put("630", "Banco Smartbank");
        banks.put("637", "Banco Sofisa");
        banks.put("643", "Banco Pine");
        banks.put("633", "Banco Rendimento");
        banks.put("376", "Banco J.P. Morgan");
        banks.put("394", "Banco Bradesco Financiamentos");
        banks.put("464", "Banco Sumitomo Mitsui Brasileiro");
        banks.put("479", "Banco ItauBank");
        banks.put("613", "Omni Banco");
        banks.put("739", "Banco Cetelem");
        banks.put("741", "Banco Ribeirão Preto");
        
        BANKS = Collections.unmodifiableMap(banks);
    }

    /**
     * Retorna o mapa imutável com todos os bancos
     */
    public static Map<String, String> getAllBanks() {
        return BANKS;
    }

    /**
     * Retorna o conjunto de todos os códigos de bancos válidos
     */
    public static Set<String> getAllBankCodes() {
        return BANKS.keySet();
    }

    /**
     * Verifica se um código de banco é válido
     * 
     * @param code Código do banco (3 dígitos)
     * @return true se o código existe no mapa de bancos
     */
    public static boolean isValidBankCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return BANKS.containsKey(code.trim());
    }

    /**
     * Retorna o nome do banco a partir do código
     * 
     * @param code Código do banco (3 dígitos)
     * @return Nome do banco ou "Banco desconhecido (código: XXX)" se não encontrado
     */
    public static String getBankName(String code) {
        if (code == null || code.isBlank()) {
            return "Código de banco inválido";
        }
        
        String trimmedCode = code.trim();
        return BANKS.getOrDefault(trimmedCode, "Banco desconhecido (código: " + trimmedCode + ")");
    }

    /**
     * Valida se o código tem formato correto (3 dígitos numéricos)
     * 
     * @param code Código a validar
     * @return true se tem exatamente 3 dígitos
     */
    public static boolean hasValidFormat(String code) {
        if (code == null) {
            return false;
        }
        return code.matches("^\\d{3}$");
    }

    /**
     * Valida se o código tem formato correto E existe no cadastro
     * 
     * @param code Código a validar
     * @return true se formato está correto e banco existe
     */
    public static boolean isValid(String code) {
        return hasValidFormat(code) && isValidBankCode(code);
    }

    /**
     * Retorna informações formatadas do banco
     * 
     * @param code Código do banco
     * @return String no formato "001 - Banco do Brasil" ou mensagem de erro
     */
    public static String getFormattedBankInfo(String code) {
        if (!hasValidFormat(code)) {
            return "Código inválido: deve ter 3 dígitos";
        }
        
        String name = getBankName(code);
        return code + " - " + name;
    }

    /**
     * Constantes para os bancos mais utilizados (acesso rápido)
     */
    public static final class Common {
        public static final String BANCO_DO_BRASIL = "001";
        public static final String SANTANDER = "033";
        public static final String CAIXA = "104";
        public static final String BRADESCO = "237";
        public static final String ITAU = "341";
        public static final String NUBANK = "260";
        public static final String INTER = "077";
        public static final String PAGSEGURO = "290";
        public static final String MERCADO_PAGO = "323";
        public static final String PICPAY = "380";
        public static final String BTG = "208";
        public static final String C6 = "336";
        public static final String ORIGINAL = "212";
        public static final String SAFRA = "422";
        
        private Common() {
            // Classe utilitária - não instanciável
        }
    }
}
