/**
 * Módulo de métricas.
 *
 * No expone nada: todas sus clases son package-private. Igual que "notifications", solo
 * reacciona a eventos de pedidos, así que solo depende de la API con nombre "events" de
 * "orders" (ver orders/events/package-info.java), no del módulo completo.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Métricas", allowedDependencies = "orders::events")
package com.example.store.metrics;
