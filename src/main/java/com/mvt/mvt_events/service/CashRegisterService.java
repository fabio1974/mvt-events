package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.CashRegisterMovementRepository;
import com.mvt.mvt_events.repository.CashRegisterSessionRepository;
import com.mvt.mvt_events.repository.FoodOrderRepository;
import com.mvt.mvt_events.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gerencia sessões de caixa do estabelecimento.
 * Regra: 1 sessão OPEN por client por vez (índice parcial único garante).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CashRegisterService {

    private final CashRegisterSessionRepository sessionRepo;
    private final CashRegisterMovementRepository movementRepo;
    private final UserRepository userRepo;
    private final FoodOrderRepository foodOrderRepo;

    /**
     * Abre uma sessão de caixa. Falha se já existe uma OPEN para este client.
     */
    public CashRegisterSession open(UUID clientId, UUID openedById, BigDecimal openingBalance, String notes) {
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new RuntimeException("Saldo de abertura inválido");
        }
        if (sessionRepo.findByClientIdAndStatus(clientId, CashRegisterSession.Status.OPEN).isPresent()) {
            throw new RuntimeException("Já existe um caixa aberto para este estabelecimento");
        }
        User client = userRepo.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));
        User openedBy = openedById != null
                ? userRepo.findById(openedById).orElse(null)
                : null;

        CashRegisterSession session = CashRegisterSession.builder()
                .client(client)
                .status(CashRegisterSession.Status.OPEN)
                .openingBalance(openingBalance)
                .openedAt(OffsetDateTime.now())
                .openedBy(openedBy)
                .notes(notes)
                .build();
        return sessionRepo.save(session);
    }

    /**
     * Fecha a sessão OPEN do client. Calcula o saldo esperado a partir das vendas em dinheiro
     * + fundo + adições - retiradas/sangrias durante a sessão.
     */
    public CashRegisterSession close(UUID clientId, UUID closedById, BigDecimal closingBalanceActual, String notes) {
        if (closingBalanceActual == null || closingBalanceActual.signum() < 0) {
            throw new RuntimeException("Saldo de fechamento inválido");
        }
        CashRegisterSession session = sessionRepo.findOpenWithMovements(clientId)
                .orElseThrow(() -> new RuntimeException("Não há caixa aberto para fechar"));

        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal expected = computeExpected(session, now);

        session.setStatus(CashRegisterSession.Status.CLOSED);
        session.setClosedAt(now);
        session.setClosingBalanceActual(closingBalanceActual);
        session.setClosingBalanceExpected(expected);
        if (closedById != null) {
            session.setClosedBy(userRepo.findById(closedById).orElse(null));
        }
        if (notes != null && !notes.isBlank()) {
            session.setNotes((session.getNotes() == null ? "" : session.getNotes() + "\n") + notes);
        }
        return sessionRepo.save(session);
    }

    /**
     * Adiciona movimentação manual (suprimento, retirada ou sangria) à sessão OPEN.
     */
    public CashRegisterMovement addMovement(UUID clientId, UUID createdById,
                                             CashRegisterMovement.Type type,
                                             BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new RuntimeException("Valor inválido");
        }
        CashRegisterSession session = sessionRepo.findByClientIdAndStatus(clientId, CashRegisterSession.Status.OPEN)
                .orElseThrow(() -> new RuntimeException("Não há caixa aberto"));

        CashRegisterMovement m = CashRegisterMovement.builder()
                .session(session)
                .type(type)
                .amount(amount)
                .reason(reason)
                .createdAt(OffsetDateTime.now())
                .createdBy(createdById != null ? userRepo.findById(createdById).orElse(null) : null)
                .build();
        return movementRepo.save(m);
    }

    @Transactional(readOnly = true)
    public Optional<CashRegisterSession> getOpenSession(UUID clientId) {
        return sessionRepo.findOpenWithMovements(clientId);
    }

    @Transactional(readOnly = true)
    public List<CashRegisterMovement> getMovements(Long sessionId) {
        return movementRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * Sessão que cobre o intervalo (ou a mais recente que se sobrepõe).
     * Útil para o relatório: pega o caixa daquele dia.
     */
    @Transactional(readOnly = true)
    public Optional<CashRegisterSession> findForRange(UUID clientId, OffsetDateTime start, OffsetDateTime end) {
        return sessionRepo.findOverlapping(clientId, start, end).stream().findFirst();
    }

    /**
     * Calcula o saldo esperado: fundo + vendas em dinheiro + adições − retiradas/sangrias.
     * Vendas em dinheiro: pedidos COMPLETED do client entre opened_at e nowOrEnd com payment=CASH.
     */
    public BigDecimal computeExpected(CashRegisterSession session, OffsetDateTime asOf) {
        BigDecimal fundo = session.getOpeningBalance() != null ? session.getOpeningBalance() : BigDecimal.ZERO;

        BigDecimal additions = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        if (session.getMovements() != null) {
            for (CashRegisterMovement m : session.getMovements()) {
                if (m.getType() == CashRegisterMovement.Type.ADDITION) {
                    additions = additions.add(m.getAmount());
                } else {
                    withdrawals = withdrawals.add(m.getAmount());
                }
            }
        }

        BigDecimal cashSales = computeCashSales(session.getClient().getId(), session.getOpenedAt(), asOf);
        return fundo.add(cashSales).add(additions).subtract(withdrawals);
    }

    private BigDecimal computeCashSales(UUID clientId, OffsetDateTime start, OffsetDateTime end) {
        List<FoodOrder> orders = foodOrderRepo.findCompletedForReport(clientId, start, end);
        BigDecimal total = BigDecimal.ZERO;
        for (FoodOrder o : orders) {
            PaymentMethod pm = pickPaymentMethod(o);
            if (pm == PaymentMethod.CASH) {
                total = total.add(o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO);
            }
        }
        return total;
    }

    private PaymentMethod pickPaymentMethod(FoodOrder o) {
        if (o.getTablePaymentMethod() != null) return o.getTablePaymentMethod();
        if (o.getMesaPaymentMethod() != null) return o.getMesaPaymentMethod();
        if (o.getItems() != null) {
            for (OrderItem item : o.getItems()) {
                if (item.getCommand() != null && item.getCommand().getPaymentMethod() != null) {
                    return item.getCommand().getPaymentMethod();
                }
            }
        }
        return PaymentMethod.NOT_INFORMED;
    }
}
