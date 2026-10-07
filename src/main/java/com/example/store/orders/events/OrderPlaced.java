package com.example.store.orders.events;

import java.util.UUID;

/**
 * Evento de dominio (DDD): algo que ya pasó. Se publica cuando un pedido queda registrado.
 * Vive en su propio paquete, con nombre ("events"), para poder exponerse sin exponer el resto
 * de la API del módulo.
 */
public record OrderPlaced(UUID orderId, String sku, int quantity, String customerEmail) {}
