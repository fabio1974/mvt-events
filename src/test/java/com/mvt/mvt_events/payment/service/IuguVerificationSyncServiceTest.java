package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.BankAccount.AccountType;
import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.SubAccountResponse;
import com.mvt.mvt_events.repository.BankAccountRepository;
import com.mvt.mvt_events.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para IuguVerificationSyncService
 * 
 * Testa:
 * - Sincronização de contas pendentes
 * - Transições de status (pending → verified/rejected)
 * - Integração com push notifications
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IuguVerificationSyncService Tests")
class IuguVerificationSyncServiceTest {

    @Mock
    private IuguService iuguService;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private IuguVerificationSyncService syncService;

    private User testUser;
    private BankAccount pendingAccount;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("test_motoboy");
        testUser.setIuguAccountId("acc_TEST123");

        pendingAccount = new BankAccount();
        pendingAccount.setId(1L);
        pendingAccount.setUser(testUser);
        pendingAccount.setBankCode("260");
        pendingAccount.setBankName("Nubank");
        pendingAccount.setAgency("0001");
        pendingAccount.setAccountNumber("12345678-9");
        pendingAccount.setAccountType(AccountType.CHECKING);
        pendingAccount.setStatus(BankAccountStatus.PENDING_VALIDATION);
    }

    @Test
    @DisplayName("Deve processar conta verificada com sucesso")
    void shouldProcessVerifiedAccount() {
        // Given
        SubAccountResponse verifiedResponse = new SubAccountResponse(
            "acc_TEST123",
            "Test User",
            "test@example.com",
            true,
            true,
            "verified"
        );

        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(List.of(pendingAccount));
        when(iuguService.getSubAccountStatus("acc_TEST123"))
            .thenReturn(verifiedResponse);

        // When
        syncService.syncPendingVerifications();

        // Then
        verify(bankAccountRepository).save(argThat(ba -> 
            ba.getStatus() == BankAccountStatus.ACTIVE
        ));
        verify(pushNotificationService).notifyBankDataVerified(
            eq(testUser.getId()),
            eq("Nubank"),
            contains("5678-9")
        );
    }

    @Test
    @DisplayName("Deve processar conta rejeitada com sucesso")
    void shouldProcessRejectedAccount() {
        // Given
        SubAccountResponse rejectedResponse = new SubAccountResponse(
            "acc_TEST123",
            "Test User",
            "test@example.com",
            false,
            false,
            "rejected"
        );

        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(List.of(pendingAccount));
        when(iuguService.getSubAccountStatus("acc_TEST123"))
            .thenReturn(rejectedResponse);

        // When
        syncService.syncPendingVerifications();

        // Then
        verify(bankAccountRepository).save(argThat(ba -> 
            ba.getStatus() == BankAccountStatus.BLOCKED
        ));
        verify(pushNotificationService).notifyBankDataRejected(
            eq(testUser.getId()),
            anyString()
        );
    }

    @Test
    @DisplayName("Deve manter conta como pendente se Iugu ainda não verificou")
    void shouldKeepAccountPendingIfStillPending() {
        // Given
        SubAccountResponse pendingResponse = new SubAccountResponse(
            "acc_TEST123",
            "Test User",
            "test@example.com",
            false,
            false,
            "pending"
        );

        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(List.of(pendingAccount));
        when(iuguService.getSubAccountStatus("acc_TEST123"))
            .thenReturn(pendingResponse);

        // When
        syncService.syncPendingVerifications();

        // Then
        verify(bankAccountRepository, never()).save(any());
        verify(pushNotificationService, never()).notifyBankDataVerified(any(), any(), any());
        verify(pushNotificationService, never()).notifyBankDataRejected(any(), any());
    }

    @Test
    @DisplayName("Deve processar múltiplas contas pendentes")
    void shouldProcessMultiplePendingAccounts() {
        // Given
        BankAccount account1 = createTestAccount("acc_TEST1", "verified");
        BankAccount account2 = createTestAccount("acc_TEST2", "rejected");
        BankAccount account3 = createTestAccount("acc_TEST3", "pending");

        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(Arrays.asList(account1, account2, account3));

        when(iuguService.getSubAccountStatus("acc_TEST1"))
            .thenReturn(new SubAccountResponse("acc_TEST1", "User1", "u1@test.com", true, true, "verified"));
        when(iuguService.getSubAccountStatus("acc_TEST2"))
            .thenReturn(new SubAccountResponse("acc_TEST2", "User2", "u2@test.com", false, false, "rejected"));
        when(iuguService.getSubAccountStatus("acc_TEST3"))
            .thenReturn(new SubAccountResponse("acc_TEST3", "User3", "u3@test.com", false, false, "pending"));

        // When
        syncService.syncPendingVerifications();

        // Then
        verify(bankAccountRepository, times(2)).save(any()); // verified + rejected
        verify(pushNotificationService, times(1)).notifyBankDataVerified(any(), any(), any());
        verify(pushNotificationService, times(1)).notifyBankDataRejected(any(), any());
    }

    @Test
    @DisplayName("Deve continuar processamento mesmo com erro em uma conta")
    void shouldContinueProcessingOnError() {
        // Given
        BankAccount account1 = createTestAccount("acc_TEST1", "verified");
        BankAccount account2 = createTestAccount("acc_TEST2", "verified");

        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(Arrays.asList(account1, account2));

        when(iuguService.getSubAccountStatus("acc_TEST1"))
            .thenThrow(new RuntimeException("API Error"));
        when(iuguService.getSubAccountStatus("acc_TEST2"))
            .thenReturn(new SubAccountResponse("acc_TEST2", "User2", "u2@test.com", true, true, "verified"));

        // When
        syncService.syncPendingVerifications();

        // Then - Deve processar account2 mesmo com erro em account1
        verify(iuguService, times(2)).getSubAccountStatus(anyString());
        verify(bankAccountRepository, times(1)).save(any()); // Apenas account2
    }

    @Test
    @DisplayName("Deve não fazer nada se não houver contas pendentes")
    void shouldDoNothingIfNoPendingAccounts() {
        // Given
        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(Collections.emptyList());

        // When
        syncService.syncPendingVerifications();

        // Then
        verify(iuguService, never()).getSubAccountStatus(anyString());
        verify(bankAccountRepository, never()).save(any());
        verify(pushNotificationService, never()).notifyBankDataVerified(any(), any(), any());
    }

    @Test
    @DisplayName("Deve lidar com erro ao enviar push notification")
    void shouldHandlePushNotificationError() {
        // Given
        SubAccountResponse verifiedResponse = new SubAccountResponse(
            "acc_TEST123",
            "Test User",
            "test@example.com",
            true,
            true,
            "verified"
        );

        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(List.of(pendingAccount));
        when(iuguService.getSubAccountStatus("acc_TEST123"))
            .thenReturn(verifiedResponse);
        doThrow(new RuntimeException("Push notification failed"))
            .when(pushNotificationService).notifyBankDataVerified(any(), any(), any());

        // When
        syncService.syncPendingVerifications();

        // Then - Deve salvar o status mesmo com erro na notificação
        verify(bankAccountRepository).save(argThat(ba -> 
            ba.getStatus() == BankAccountStatus.ACTIVE
        ));
    }

    @Test
    @DisplayName("Deve mascarar número da conta corretamente")
    void shouldMaskAccountNumberCorrectly() {
        // Given
        pendingAccount.setAccountNumber("98765432-1");
        
        SubAccountResponse verifiedResponse = new SubAccountResponse(
            "acc_TEST123",
            "Test User",
            "test@example.com",
            true,
            true,
            "verified"
        );

        when(bankAccountRepository.findByStatus(BankAccountStatus.PENDING_VALIDATION))
            .thenReturn(List.of(pendingAccount));
        when(iuguService.getSubAccountStatus("acc_TEST123"))
            .thenReturn(verifiedResponse);

        // When
        syncService.syncPendingVerifications();

        // Then
        verify(pushNotificationService).notifyBankDataVerified(
            eq(testUser.getId()),
            eq("Nubank"),
            argThat(masked -> 
                masked.contains("****") && 
                masked.contains("5432-1") &&
                !masked.contains("9876")
            )
        );
    }

    // Helper methods
    private BankAccount createTestAccount(String iuguAccountId, String verificationStatus) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("user_" + iuguAccountId);
        user.setIuguAccountId(iuguAccountId);

        BankAccount account = new BankAccount();
        account.setId(System.currentTimeMillis());
        account.setUser(user);
        account.setBankCode("260");
        account.setBankName("Nubank");
        account.setAgency("0001");
        account.setAccountNumber("12345678-9");
        account.setAccountType(AccountType.CHECKING);
        account.setStatus(BankAccountStatus.PENDING_VALIDATION);

        return account;
    }
}
