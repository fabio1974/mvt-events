package com.mvt.mvt_events.payment.dto;

import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;

/**
 * DTO de resposta para verificação de status da conta
 * 
 * <p>Usado em:
 * <ul>
 *   <li>GET /api/motoboy/bank-data/verification-status - Verificar status em tempo real</li>
 * </ul>
 */
public record VerificationStatusResponse(
    String iuguAccountId,
    BankAccountStatus localStatus,
    String localStatusDisplayName,
    String iuguVerificationStatus,
    Boolean isVerified,
    Boolean isPending,
    Boolean isRejected,
    Boolean canReceivePayments,
    String message
) {
    
    /**
     * Cria resposta de verificação com status local e remoto
     */
    public static VerificationStatusResponse of(
            String iuguAccountId,
            BankAccountStatus localStatus,
            String iuguVerificationStatus
    ) {
        boolean isVerified = "verified".equalsIgnoreCase(iuguVerificationStatus);
        boolean isPending = "pending".equalsIgnoreCase(iuguVerificationStatus);
        boolean isRejected = "rejected".equalsIgnoreCase(iuguVerificationStatus);
        boolean canReceive = isVerified && localStatus == BankAccountStatus.ACTIVE;
        
        String message = buildMessage(localStatus, iuguVerificationStatus);
        
        return new VerificationStatusResponse(
            iuguAccountId,
            localStatus,
            localStatus.getDisplayName(),
            iuguVerificationStatus,
            isVerified,
            isPending,
            isRejected,
            canReceive,
            message
        );
    }
    
    /**
     * Cria resposta quando não há dados bancários cadastrados
     */
    public static VerificationStatusResponse notRegistered() {
        return new VerificationStatusResponse(
            null,
            null,
            null,
            null,
            false,
            false,
            false,
            false,
            "Dados bancários não cadastrados. Por favor, cadastre seus dados antes de verificar o status."
        );
    }
    
    /**
     * Cria resposta quando não há iuguAccountId
     */
    public static VerificationStatusResponse notLinkedToIugu() {
        return new VerificationStatusResponse(
            null,
            BankAccountStatus.PENDING_VALIDATION,
            "Pendente de Validação",
            "pending",
            false,
            true,
            false,
            false,
            "Subconta Iugu ainda não foi criada. Por favor, aguarde o processamento."
        );
    }
    
    private static String buildMessage(BankAccountStatus localStatus, String iuguStatus) {
        if ("verified".equalsIgnoreCase(iuguStatus) && localStatus == BankAccountStatus.ACTIVE) {
            return "✅ Seus dados bancários foram verificados! Você já pode receber pagamentos via PIX.";
        }
        
        if ("pending".equalsIgnoreCase(iuguStatus)) {
            return "⏳ Seus dados bancários estão em verificação. Esse processo pode levar de 2 a 5 dias úteis.";
        }
        
        if ("rejected".equalsIgnoreCase(iuguStatus)) {
            return "❌ Seus dados bancários foram rejeitados. Por favor, revise e atualize as informações.";
        }
        
        if (localStatus == BankAccountStatus.BLOCKED) {
            return "🔒 Sua conta bancária está bloqueada. Entre em contato com o suporte.";
        }
        
        return "⏳ Verificando status da sua conta bancária...";
    }
}
