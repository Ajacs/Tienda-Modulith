package com.example.store.inventory.internal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.inventory.Inventory;
import com.example.store.inventory.StockLevel;

@RestController
@RequestMapping("/inventory")
class InventoryController {

    private final Inventory inventory;

    InventoryController(Inventory inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    List<StockLevel> levels() {
        return inventory.levels();
    }
}
