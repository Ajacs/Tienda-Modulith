package com.example.store.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.example.store.orders.events.OrderPlaced;

/**
 * Cada pedido registrado incrementa un contador. No toca el módulo de pedidos para nada:
 * solo escucha {@link OrderPlaced}, igual que "notifications". Así se ve en vivo que agregar
 * un nuevo consumidor de un evento de dominio no cuesta tocar el módulo que lo publica.
 *
 * Contador expuesto en Prometheus como "pedidos_recibidos_total", con el sku como tag
 * (cuidado con la cardinalidad en un sistema real: acá son 3 SKUs, es solo para el demo).
 */
@Component
class OrderMetrics {

    private final MeterRegistry meters;

    OrderMetrics(MeterRegistry meters) {
        this.meters = meters;
    }

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        meters.counter("pedidos.recibidos", "sku", event.sku()).increment();
    }
}
