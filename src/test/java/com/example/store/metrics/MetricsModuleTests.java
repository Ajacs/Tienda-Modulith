package com.example.store.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import com.example.store.orders.events.OrderPlaced;

/**
 * Levanta SOLO el módulo de métricas. Publica el evento directamente, sin crear un pedido real,
 * y verifica que el contador de Micrometer avanza.
 */
@ApplicationModuleTest
class MetricsModuleTests {

    @Autowired MeterRegistry meters;

    @Test
    void incrementsCounterWhenAnOrderIsPlaced(Scenario scenario) {

        var event = new OrderPlaced(UUID.randomUUID(), "CAFE-001", 2, "ana@example.com");
        double before = countFor("CAFE-001");

        scenario.publish(event)
                .andWaitForStateChange(() -> countFor("CAFE-001"))
                .andVerify(after -> assertThat(after).isEqualTo(before + 1.0));
    }

    private double countFor(String sku) {
        var counter = meters.find("pedidos.recibidos").tag("sku", sku).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
