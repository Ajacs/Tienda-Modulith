package com.example.store.inventory;

/**
 * Excepción de dominio: cruza del agregado {@code StockItem} hacia afuera del módulo,
 * así que vive en la API pública, junto a {@link Inventory}.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requested, int available) {
        super("Stock insuficiente para %s: se pidieron %d y hay %d".formatted(sku, requested, available));
    }
}
