package com.example.store.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import com.example.store.orders.events.OrderPlaced;

/**
 * Levanta SOLO el módulo de notificaciones. No hace falta crear un pedido real:
 * se publica el evento y se espera el efecto.
 */
@ApplicationModuleTest
class NotificationsModuleTests {

    @Autowired ConfirmationMailer mailer;

    @Test
    void sendsConfirmationWhenAnOrderIsPlaced(Scenario scenario) {

        var event = new OrderPlaced(UUID.randomUUID(), "CAFE-001", 2, "ana@example.com");

        scenario.publish(event)
                .andWaitForStateChange(() -> mailer.wasSentTo("ana@example.com"))
                .andVerify(sent -> assertThat(sent).isTrue());
    }
}
