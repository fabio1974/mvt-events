package com.mvt.mvt_events.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Regra de split de pagamento no Iugu
 * 
 * <p>Define quanto cada subconta deve receber.</p>
 */
@Data
public class SplitRequest {

    /**
     * ID da subconta que receberá o split
     */
    @JsonProperty("receiver_id")
    private String receiverId;

    /**
     * Valor em centavos (mutuamente exclusivo com percent)
     */
    @JsonProperty("cents")
    private Integer cents;

    /**
     * Percentual (mutuamente exclusivo com cents)
     */
    @JsonProperty("percent")
    private Integer percent;
}
