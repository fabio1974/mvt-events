package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.BankAccount.BankAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para BankAccount.
 * Gerencia dados bancários de couriers e organizers para pagamentos via Pagar.me.
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * Busca conta bancária por ID do usuário (relacionamento 1:1)
     */
    Optional<BankAccount> findByUserId(UUID userId);

    /**
     * Verifica se usuário já possui conta bancária cadastrada
     */
    boolean existsByUserId(UUID userId);

    /**
     * Busca todas as contas bancárias por status
     */
    List<BankAccount> findByStatus(BankAccountStatus status);

    /**
     * Busca contas bancárias ativas
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.status = 'ACTIVE'")
    List<BankAccount> findAllActive();

    /**
     * Busca contas bancárias pendentes de validação
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.status = 'PENDING_VALIDATION' ORDER BY ba.createdAt ASC")
    List<BankAccount> findAllPendingValidation();

    /**
     * Busca contas bancárias de um banco específico
     */
    List<BankAccount> findByBankCode(String bankCode);

    /**
     * Busca contas bancárias pelo número da conta (usado para validação de duplicidade)
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.bankCode = :bankCode AND ba.agency = :agency AND ba.accountNumber = :accountNumber")
    Optional<BankAccount> findByBankDetails(
            @Param("bankCode") String bankCode,
            @Param("agency") String agency,
            @Param("accountNumber") String accountNumber
    );

    /**
     * Verifica se já existe outra conta bancária com os mesmos dados
     * (evita cadastro duplicado)
     */
    @Query("SELECT CASE WHEN COUNT(ba) > 0 THEN true ELSE false END FROM BankAccount ba " +
           "WHERE ba.bankCode = :bankCode AND ba.agency = :agency AND ba.accountNumber = :accountNumber " +
           "AND ba.user.id != :userId")
    boolean existsDuplicateAccount(
            @Param("bankCode") String bankCode,
            @Param("agency") String agency,
            @Param("accountNumber") String accountNumber,
            @Param("userId") UUID userId
    );

    /**
     * Conta quantas contas bancárias ativas existem
     */
    @Query("SELECT COUNT(ba) FROM BankAccount ba WHERE ba.status = 'ACTIVE'")
    long countActive();

    /**
     * Conta quantas contas bancárias estão pendentes
     */
    @Query("SELECT COUNT(ba) FROM BankAccount ba WHERE ba.status = 'PENDING_VALIDATION'")
    long countPending();

    /**
     * Deleta conta bancária por ID do usuário
     */
    void deleteByUserId(UUID userId);
}
