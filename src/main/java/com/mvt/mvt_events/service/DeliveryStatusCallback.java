package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Delivery;

/**
 * Callback invocado pelo DeliveryService quando o status de uma Delivery muda.
 * Permite que módulos dependentes (ex: FoodOrder) reajam sem criar dependência circular.
 */
public interface DeliveryStatusCallback {

    /**
     * Chamado após uma delivery mudar de status.
     * @param delivery a delivery com o novo status já persistido
     */
    void onDeliveryStatusChanged(Delivery delivery);
}
