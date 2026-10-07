package com.example.store.orders;

import java.util.UUID;

/**
 * Vista de un pedido hacia afuera del módulo. El agregado {@code Order} se queda en "internal".
 */
public record OrderSummary(UUID id, String sku, int quantity, String customerEmail) {}
