package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.store.inventory.InsufficientStockException;
import com.example.store.inventory.Inventory;
import com.example.store.orders.OrderManagement;

/**
 * Aplicación completa: pedidos e inventario comparten una transacción local.
 */
@SpringBootTest
class StoreIntegrationTests {

    @Autowired OrderManagement orders;
    @Autowired Inventory inventory;

    @Test
    void placingAnOrderReservesStock() {

        int stockBefore = inventory.available("TAZA-002");

        orders.place("TAZA-002", 2, "ana@example.com");

        assertThat(inventory.available("TAZA-002")).isEqualTo(stockBefore - 2);
    }

    @Test
    void rollsBackTheOrderWhenThereIsNotEnoughStock() {

        int ordersBefore = orders.findAll().size();
        int stockBefore = inventory.available("CAFE-001");

        assertThatThrownBy(() -> orders.place("CAFE-001", 999, "ana@example.com"))
                .isInstanceOf(InsufficientStockException.class);

        // Sin saga ni compensación: el pedido no existe y el stock no cambió.
        assertThat(orders.findAll()).hasSize(ordersBefore);
        assertThat(inventory.available("CAFE-001")).isEqualTo(stockBefore);
    }
}
