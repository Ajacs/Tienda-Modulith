package com.example.store.notifications;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.example.store.orders.events.OrderPlaced;

@Component
class OrderNotifications {

    // DEMO (romper el límite): descomenta la línea de abajo y corre ModularityTests.
    // private com.example.store.orders.internal.OrderRepository atajo;

    private final ConfirmationMailer mailer;

    OrderNotifications(ConfirmationMailer mailer) {
        this.mailer = mailer;
    }

    /**
     * @ApplicationModuleListener = después del commit + asíncrono + transacción propia.
     * Si este método falla, el pedido ya está confirmado y el evento queda registrado como
     * pendiente en el outbox (tabla EVENT_PUBLICATION).
     */
    @ApplicationModuleListener
    void on(OrderPlaced event) {
        mailer.send(event.customerEmail(),
                "Recibimos tu pedido %s: %d x %s".formatted(event.orderId(), event.quantity(), event.sku()));
    }
}
