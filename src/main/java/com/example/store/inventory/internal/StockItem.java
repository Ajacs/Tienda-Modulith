package com.example.store.inventory.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.example.store.inventory.InsufficientStockException;

/**
 * Agregado (DDD) "Item de stock": encapsula su propia invariante (no reservar más de lo
 * disponible). {@code Inventory} nunca manipula {@code available} directamente.
 */
@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    private String sku;

    private int available;

    protected StockItem() {
        // requerido por JPA
    }

    public StockItem(String sku, int available) {
        this.sku = sku;
        this.available = available;
    }

    public void reserve(int quantity) {
        if (quantity > available) {
            throw new InsufficientStockException(sku, quantity, available);
        }
        available -= quantity;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }
}
