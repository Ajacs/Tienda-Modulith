package com.example.store.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio (DDD) del agregado {@link StockItem}.
 */
public interface StockRepository extends JpaRepository<StockItem, String> {}
