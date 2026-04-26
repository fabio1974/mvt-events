package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.PagarmeTransfer;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.PagarmeTransferRepository;
import com.mvt.mvt_events.service.CourierTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin: "Dívidas com Couriers" — lista e ações manuais sobre PagarmeTransfer PENDING.
 *
 * Modo semi-automático:
 *  - Courier aceita delivery de FoodOrder paga → BE cria Transfer PENDING (débito)
 *  - Admin vê na tela, confere, e toma 1 de 3 ações:
 *     1) "Enviar PIX" → dispara PixOutProvider (LogOnly em dev, Stark/Inter em prod)
 *     2) "Marcar Pago" → admin já mandou PIX manualmente, só registra
 *     3) "Cancelar" → marca FAILED com motivo
 */
@RestController
@RequestMapping("/api/pagarme-transfers")
@Tag(name = "PagarmeTransfers", description = "Dívidas plataforma → courier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PagarmeTransferController {

    private final PagarmeTransferRepository repository;
    private final CourierTransferService courierTransferService;

    @GetMapping
    @Operation(summary = "Listar transfers (filtro por status, default PENDING)")
    public List<PagarmeTransfer> list(@RequestParam(required = false, defaultValue = "PENDING") String status) {
        PagarmeTransfer.Status st;
        try { st = PagarmeTransfer.Status.valueOf(status.toUpperCase()); }
        catch (Exception e) { throw new RuntimeException("Status inválido: " + status); }
        return repository.findByStatusWithRecipient(st);
    }

    /**
     * Total e detalhes de transfers PENDING do usuário logado.
     * <p>Usado pela tela "Minha Carteira" do mobile (courier/organizer): mostra dinheiro
     * já garantido (cliente pagou) mas ainda na conta da plataforma, esperando PIX-out.
     * <p>Acessível por COURIER e ORGANIZER (não só ADMIN).
     */
    @GetMapping("/my-pending")
    @PreAuthorize("hasAnyRole('COURIER', 'ORGANIZER', 'ADMIN')")
    @Operation(summary = "Meus transfers PENDING (a receber via PIX-out da plataforma)")
    public MyPendingTransfersResponse myPending(
            org.springframework.security.core.Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<PagarmeTransfer> transfers = repository.findByRecipientIdAndStatus(
                currentUser.getId(), PagarmeTransfer.Status.PENDING);

        long totalCents = transfers.stream().mapToLong(PagarmeTransfer::getAmountCents).sum();

        List<PendingTransferItem> items = transfers.stream().map(t -> {
            PendingTransferItem item = new PendingTransferItem();
            item.id = t.getId();
            item.foodOrderId = t.getFoodOrder() != null ? t.getFoodOrder().getId() : null;
            item.deliveryId = t.getDeliveryId();
            item.amountCents = t.getAmountCents();
            item.createdAt = t.getCreatedAt();
            return item;
        }).collect(Collectors.toList());

        MyPendingTransfersResponse resp = new MyPendingTransfersResponse();
        resp.totalCents = totalCents;
        resp.totalBrl = totalCents / 100.0;
        resp.count = items.size();
        resp.transfers = items;
        return resp;
    }

    @GetMapping("/by-recipient")
    @Operation(summary = "Agregado por destinatário (total PENDING por pessoa — motoboy ou gerente)")
    public List<RecipientDebt> byRecipient() {
        List<PagarmeTransfer> pending = repository.findByStatusWithRecipient(PagarmeTransfer.Status.PENDING);
        Map<UUID, RecipientDebt> byRecipient = new LinkedHashMap<>();
        for (PagarmeTransfer t : pending) {
            User c = t.getRecipient();
            if (c == null) continue;
            RecipientDebt d = byRecipient.computeIfAbsent(c.getId(), k -> {
                RecipientDebt cd = new RecipientDebt();
                cd.recipientId = c.getId();
                cd.recipientName = c.getName();
                cd.role = c.getRole() != null ? c.getRole().name() : null;
                cd.pixKey = c.getPixKey();
                cd.pixKeyType = c.getPixKeyType() != null ? c.getPixKeyType().name() : null;
                cd.transfers = new ArrayList<>();
                cd.totalCents = 0L;
                return cd;
            });
            TransferSummary ts = new TransferSummary();
            ts.id = t.getId();
            ts.foodOrderId = t.getFoodOrder() != null ? t.getFoodOrder().getId() : null;
            ts.deliveryId = t.getDeliveryId();
            ts.amountCents = t.getAmountCents();
            ts.createdAt = t.getCreatedAt();
            d.transfers.add(ts);
            d.totalCents += t.getAmountCents();
        }
        return new ArrayList<>(byRecipient.values());
    }

    @PostMapping("/{id}/send-pix")
    @Operation(summary = "Dispara o PIX via PixOutProvider configurado")
    public ResponseEntity<PagarmeTransfer> sendPix(@PathVariable Long id) {
        PagarmeTransfer t = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer não encontrado"));
        if (t.getStatus() != PagarmeTransfer.Status.PENDING) {
            throw new RuntimeException("Transfer não está PENDING (status atual: " + t.getStatus() + ")");
        }
        if (t.getRecipient() == null || t.getRecipient().getPixKey() == null
                || t.getRecipient().getPixKey().isBlank()) {
            String label = t.getRecipient() != null && t.getRecipient().getRole() == User.Role.ORGANIZER
                    ? "Gerente" : "Motoboy";
            throw new RuntimeException(label + " não tem chave PIX cadastrada");
        }
        courierTransferService.executeTransfer(t, t.getRecipient());
        return ResponseEntity.ok(t);
    }

    @PostMapping("/{id}/mark-paid")
    @Operation(summary = "Admin marca como paga manualmente (já enviou PIX por fora)")
    public PagarmeTransfer markPaid(@PathVariable Long id, @RequestBody(required = false) NoteRequest body) {
        PagarmeTransfer t = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer não encontrado"));
        if (t.getStatus() != PagarmeTransfer.Status.PENDING) {
            throw new RuntimeException("Transfer não está PENDING");
        }
        t.setStatus(PagarmeTransfer.Status.SUCCEEDED);
        t.setExecutedAt(OffsetDateTime.now());
        if (body != null && body.note != null && !body.note.isBlank()) {
            t.setPagarmeTransferId("manual:" + body.note);
        } else {
            t.setPagarmeTransferId("manual");
        }
        return repository.save(t);
    }

    @PostMapping("/{id}/mark-failed")
    @Operation(summary = "Admin marca como falha (cancelar)")
    public PagarmeTransfer markFailed(@PathVariable Long id, @RequestBody(required = false) NoteRequest body) {
        PagarmeTransfer t = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer não encontrado"));
        if (t.getStatus() != PagarmeTransfer.Status.PENDING) {
            throw new RuntimeException("Transfer não está PENDING");
        }
        t.setStatus(PagarmeTransfer.Status.FAILED);
        t.setExecutedAt(OffsetDateTime.now());
        t.setErrorMessage(body != null && body.note != null ? body.note : "Cancelado por admin");
        return repository.save(t);
    }

    @Data
    public static class NoteRequest {
        public String note;
    }

    public static class RecipientDebt {
        public UUID recipientId;
        public String recipientName;
        public String role;
        public String pixKey;
        public String pixKeyType;
        public long totalCents;
        public List<TransferSummary> transfers;
    }

    public static class TransferSummary {
        public Long id;
        public Long foodOrderId;
        public Long deliveryId;
        public Long amountCents;
        public OffsetDateTime createdAt;
    }

    /** Resposta do endpoint my-pending — saldo "a receber" do usuário logado. */
    public static class MyPendingTransfersResponse {
        public long totalCents;
        public double totalBrl;
        public int count;
        public List<PendingTransferItem> transfers;
    }

    public static class PendingTransferItem {
        public Long id;
        public Long foodOrderId;
        public Long deliveryId;
        public Long amountCents;
        public OffsetDateTime createdAt;
    }
}
