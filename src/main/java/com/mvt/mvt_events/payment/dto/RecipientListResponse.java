package com.mvt.mvt_events.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response da listagem de recipients do Pagar.me
 * 
 * Exemplo:
 * {
 *   "data": [ ... ],
 *   "paging": {
 *     "total": 10,
 *     "page": 1,
 *     "pages": 1
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientListResponse {
    private List<RecipientResponse> data;
    private Paging paging;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paging {
        private Integer total;
        private Integer page;
        private Integer pages;
    }
}
