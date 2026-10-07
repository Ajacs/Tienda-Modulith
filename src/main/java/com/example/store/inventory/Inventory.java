package com.example.store.inventory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.inventory.internal.StockItem;
import com.example.store.inventory.internal.StockRepository;

/**
 * API del módulo de inventario.
 */
@Service
public class Inventory {

    private static final Logger log = LoggerFactory.getLogger(Inventory.class);

    // DEMO opcional (dependencia no permitida + ciclo): descomenta la línea de abajo y corre ModularityTests.
    // private com.example.store.orders.events.OrderPlaced atajo;

    private final StockRepository stock;

    Inventory(StockRepository stock) {
        this.stock = stock;
    }

    /**
     * Si quien llama ya tiene una transacción abierta, esta operación participa en ella.
     */
    @Transactional
    public void reserve(String sku, int quantity) {

        StockItem item = stock.findById(sku)
                .orElseThrow(() -> new InsufficientStockException(sku, quantity, 0));

        item.reserve(quantity);

        log.info("Reservadas {} unidades de {}. Quedan {}", quantity, sku, item.getAvailable());
    }

    @Transactional(readOnly = true)
    public int available(String sku) {
        return stock.findById(sku).map(StockItem::getAvailable).orElse(0);
    }

    @Transactional(readOnly = true)
    public List<StockLevel> levels() {
        return stock.findAll().stream()
                .map(item -> new StockLevel(item.getSku(), item.getAvailable()))
                .toList();
    }
}
