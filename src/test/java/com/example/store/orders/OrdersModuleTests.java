package com.example.store.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.store.inventory.InsufficientStockException;
import com.example.store.inventory.Inventory;
import com.example.store.orders.events.OrderPlaced;

/**
 * Levanta SOLO el módulo de pedidos. El módulo de inventario no se carga,
 * así que su API se sustituye con un mock. Quita el @MockitoBean y el contexto no arranca.
 */
@ApplicationModuleTest
class OrdersModuleTests {

    @Autowired OrderManagement orders;
    @MockitoBean Inventory inventory;

    @Test
    void publishesOrderPlaced(Scenario scenario) {

        scenario.stimulate(() -> orders.place("CAFE-001", 2, "ana@example.com"))
                .andWaitForEventOfType(OrderPlaced.class)
                .matching(event -> event.sku().equals("CAFE-001"))
                .toArriveAndVerify((event, orderId) -> assertThat(event.orderId()).isEqualTo(orderId));
    }

    @Test
    void doesNotKeepTheOrderWhenInventoryRejectsIt() {

        doThrow(new InsufficientStockException("CAFE-001", 99, 10))
                .when(inventory).reserve("CAFE-001", 99);

        int ordersBefore = orders.findAll().size();

        assertThatThrownBy(() -> orders.place("CAFE-001", 99, "ana@example.com"))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(orders.findAll()).hasSize(ordersBefore);
    }
}
