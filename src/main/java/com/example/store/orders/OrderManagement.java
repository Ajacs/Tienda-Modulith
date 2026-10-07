package com.example.store.orders;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.inventory.Inventory;
import com.example.store.orders.events.OrderPlaced;
import com.example.store.orders.internal.Order;
import com.example.store.orders.internal.OrderRepository;

/**
 * Servicio de aplicación (DDD) y API del módulo de pedidos: orquesta el agregado {@code Order}
 * sin tener reglas de negocio propias más allá de coordinar persistencia, inventario y eventos.
 */
@Service
public class OrderManagement {

    private static final Logger log = LoggerFactory.getLogger(OrderManagement.class);

    private final OrderRepository orders;
    private final Inventory inventory;
    private final ApplicationEventPublisher events;

    OrderManagement(OrderRepository orders, Inventory inventory, ApplicationEventPublisher events) {
        this.orders = orders;
        this.inventory = inventory;
        this.events = events;
    }

    @Transactional
    public UUID place(String sku, int quantity, String customerEmail) {

        // 1. Guardamos el pedido.
        Order order = orders.save(new Order(sku, quantity, customerEmail));

        // 2. Llamada directa a la API de otro módulo, dentro de la MISMA transacción.
        //    Si no hay stock, se lanza una excepción y el pedido del paso 1 se revierte.
        inventory.reserve(sku, quantity);

        // 3. Publicamos el evento de dominio. Los listeners con @ApplicationModuleListener
        //    se ejecutan después del commit, en otro hilo y en su propia transacción.
        //    Spring Modulith guarda esta publicación en la tabla EVENT_PUBLICATION (outbox)
        //    en la MISMA transacción que el pedido: o se confirman las dos, o ninguna.
        events.publishEvent(new OrderPlaced(order.getId(), sku, quantity, customerEmail));

        log.info("Pedido {} registrado: {} x {}", order.getId(), quantity, sku);

        return order.getId();
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> findAll() {
        return orders.findAll().stream()
                .map(order -> new OrderSummary(order.getId(), order.getSku(), order.getQuantity(), order.getCustomerEmail()))
                .toList();
    }
}
