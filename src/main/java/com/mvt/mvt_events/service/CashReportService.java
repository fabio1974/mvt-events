package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.CashReportDto;
import com.mvt.mvt_events.dto.CashReportDto.CashSummary;
import com.mvt.mvt_events.dto.CashReportDto.Channel;
import com.mvt.mvt_events.dto.CashReportDto.ChannelSummary;
import com.mvt.mvt_events.dto.CashReportDto.ItemRow;
import com.mvt.mvt_events.jpa.CashRegisterMovement;
import com.mvt.mvt_events.jpa.CashRegisterSession;
import com.mvt.mvt_events.jpa.FoodOrder;
import com.mvt.mvt_events.jpa.OrderItem;
import com.mvt.mvt_events.jpa.PaymentMethod;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.FoodOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gera o relatório diário de caixa de um estabelecimento (client).
 * Filtro: pedidos COMPLETED criados na data alvo.
 * Bucketização por canal:
 *  - BALCAO: orderType=TABLE e table.isCounter=true
 *  - MESAS:  orderType=TABLE e table não-balcão (ou null)
 *  - DELIVERY: orderType=DELIVERY
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CashReportService {

    private static final ZoneId TZ = ZoneId.of("America/Fortaleza");

    private final FoodOrderRepository foodOrderRepository;
    private final CashRegisterService cashRegisterService;

    /**
     * Gera o relatório do dia para um client. Retorna null se não houver pedidos no dia.
     */
    @Transactional(readOnly = true)
    public CashReportDto generateFor(User client, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay(TZ).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(TZ).toOffsetDateTime();
        CashReportDto dto = generateForRange(client, date, start, end);
        // Para o cron: não enviar email quando não há pedidos nem caixa registrado
        if (dto.getGrandTotal().signum() == 0
                && (dto.getCash() == null || "NONE".equals(dto.getCash().getStatus()))) {
            return null;
        }
        return dto;
    }

    /**
     * Versão com intervalo arbitrário — usado pela tela on-demand do FE com filtro HH:mm.
     * Retorna o relatório mesmo sem pedidos (zerado), pra UI mostrar o estado.
     */
    @Transactional(readOnly = true)
    public CashReportDto generateForRange(User client, LocalDate date, OffsetDateTime start, OffsetDateTime end) {
        List<FoodOrder> orders = foodOrderRepository.findCompletedForReport(client.getId(), start, end);

        Map<Channel, List<FoodOrder>> byChannel = orders.stream()
                .collect(Collectors.groupingBy(this::classify));

        List<ChannelSummary> channels = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Channel ch : List.of(Channel.BALCAO, Channel.MESAS, Channel.DELIVERY)) {
            List<FoodOrder> list = byChannel.getOrDefault(ch, List.of());
            BigDecimal items = sum(list, FoodOrder::getSubtotal);
            BigDecimal fees = sum(list, FoodOrder::getDeliveryFee);
            BigDecimal total = sum(list, FoodOrder::getTotal);
            channels.add(ChannelSummary.builder()
                    .channel(ch)
                    .orderCount(list.size())
                    .itemsTotal(items)
                    .deliveryFeeTotal(fees)
                    .total(total)
                    .build());
            grandTotal = grandTotal.add(total);
        }

        Map<PaymentMethod, BigDecimal> paymentMethods = aggregatePaymentMethods(orders);
        List<ItemRow> items = aggregateItems(orders);
        CashSummary cash = buildCashSummary(client.getId(), start, end);

        return CashReportDto.builder()
                .storeName(client.getName())
                .storeDocument(safeDocument(client))
                .storeAddress(safeAddress(client))
                .date(date)
                .channels(channels)
                .paymentMethods(paymentMethods)
                .items(items)
                .grandTotal(grandTotal)
                .cash(cash)
                .build();
    }

    /**
     * Resumo de caixa do período. Pega a sessão que cobre o intervalo (geralmente do dia).
     * Retorna status NONE se não houver sessão.
     */
    private CashSummary buildCashSummary(java.util.UUID clientId, OffsetDateTime start, OffsetDateTime end) {
        return cashRegisterService.findForRange(clientId, start, end)
                .map(s -> {
                    BigDecimal additions = BigDecimal.ZERO;
                    BigDecimal withdrawals = BigDecimal.ZERO;
                    if (s.getMovements() != null) {
                        for (CashRegisterMovement m : s.getMovements()) {
                            if (m.getType() == CashRegisterMovement.Type.ADDITION) {
                                additions = additions.add(m.getAmount());
                            } else {
                                withdrawals = withdrawals.add(m.getAmount());
                            }
                        }
                    }
                    OffsetDateTime asOf = s.getClosedAt() != null ? s.getClosedAt() : end;
                    BigDecimal expected = cashRegisterService.computeExpected(s, asOf);
                    BigDecimal cashSales = expected
                            .subtract(s.getOpeningBalance() != null ? s.getOpeningBalance() : BigDecimal.ZERO)
                            .subtract(additions)
                            .add(withdrawals);
                    return CashSummary.builder()
                            .openingBalance(s.getOpeningBalance())
                            .additions(additions)
                            .withdrawals(withdrawals)
                            .cashSales(cashSales)
                            .expectedBalance(expected)
                            .actualBalance(s.getClosingBalanceActual())
                            .status(s.getStatus().name())
                            .build();
                })
                .orElse(CashSummary.builder()
                        .openingBalance(BigDecimal.ZERO)
                        .additions(BigDecimal.ZERO)
                        .withdrawals(BigDecimal.ZERO)
                        .cashSales(BigDecimal.ZERO)
                        .expectedBalance(BigDecimal.ZERO)
                        .actualBalance(null)
                        .status("NONE")
                        .build());
    }

    private Channel classify(FoodOrder o) {
        if (o.getOrderType() == FoodOrder.OrderType.DELIVERY) return Channel.DELIVERY;
        if (o.getTable() != null && Boolean.TRUE.equals(o.getTable().getIsCounter())) return Channel.BALCAO;
        return Channel.MESAS;
    }

    private BigDecimal sum(List<FoodOrder> orders, java.util.function.Function<FoodOrder, BigDecimal> f) {
        return orders.stream()
                .map(f)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Agrega forma de pagamento.
     * Ordem de prioridade por order:
     *  1. Se há OrderCommands com paymentMethod, soma cada uma proporcional ao total da comanda.
     *  2. Caso contrário, usa tablePaymentMethod (ou mesaPaymentMethod) como fallback no total da order.
     *  3. Se nada estiver setado, classifica como NOT_INFORMED.
     */
    private Map<PaymentMethod, BigDecimal> aggregatePaymentMethods(List<FoodOrder> orders) {
        Map<PaymentMethod, BigDecimal> map = new LinkedHashMap<>();
        for (FoodOrder o : orders) {
            BigDecimal total = o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO;
            PaymentMethod pm = pickPaymentMethod(o);
            map.merge(pm, total, BigDecimal::add);
        }
        return map;
    }

    private PaymentMethod pickPaymentMethod(FoodOrder o) {
        if (o.getTablePaymentMethod() != null) return o.getTablePaymentMethod();
        if (o.getMesaPaymentMethod() != null) return o.getMesaPaymentMethod();
        // Fallback: primeira comanda paga
        if (o.getItems() != null) {
            for (OrderItem item : o.getItems()) {
                if (item.getCommand() != null && item.getCommand().getPaymentMethod() != null) {
                    return item.getCommand().getPaymentMethod();
                }
            }
        }
        return PaymentMethod.NOT_INFORMED;
    }

    private List<ItemRow> aggregateItems(List<FoodOrder> orders) {
        Map<Long, ItemRow> byProduct = new LinkedHashMap<>();
        for (FoodOrder o : orders) {
            if (o.getItems() == null) continue;
            for (OrderItem item : o.getItems()) {
                if (item.getProduct() == null) continue;
                Long pid = item.getProduct().getId();
                BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                ItemRow row = byProduct.get(pid);
                if (row == null) {
                    byProduct.put(pid, ItemRow.builder()
                            .productId(pid)
                            .productName(item.getProduct().getName())
                            .quantity(item.getQuantity())
                            .total(lineTotal)
                            .build());
                } else {
                    row.setQuantity(row.getQuantity() + item.getQuantity());
                    row.setTotal(row.getTotal().add(lineTotal));
                }
            }
        }
        return byProduct.values().stream()
                .sorted(Comparator.comparing(ItemRow::getQuantity).reversed())
                .collect(Collectors.toList());
    }

    private String safeDocument(User u) {
        try { return u.getDocumentFormatted(); } catch (Exception e) { return null; }
    }

    private String safeAddress(User u) {
        try {
            var a = u.getAddress();
            return a != null ? a.getFullAddress() : null;
        } catch (Exception e) { return null; }
    }
}
