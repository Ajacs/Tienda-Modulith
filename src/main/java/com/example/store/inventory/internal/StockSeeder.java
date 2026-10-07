package com.example.store.inventory.internal;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales para el demo.
 */
@Component
class StockSeeder implements ApplicationRunner {

    private final StockRepository stock;

    StockSeeder(StockRepository stock) {
        this.stock = stock;
    }

    @Override
    public void run(ApplicationArguments args) {
        stock.saveAll(List.of(
                new StockItem("CAFE-001", 10),
                new StockItem("TAZA-002", 5),
                new StockItem("FILTRO-003", 0)));
    }
}
