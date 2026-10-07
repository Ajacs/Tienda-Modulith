/**
 * API con nombre (named interface) del módulo de pedidos: solo expone sus eventos de dominio.
 * Un módulo que únicamente necesite escuchar eventos (como "notifications") puede declarar
 * {@code allowedDependencies = "orders::events"} en vez de "orders" y así no ve
 * {@code OrderManagement} ni {@code OrderSummary}, aunque estén en el mismo módulo.
 */
@org.springframework.modulith.NamedInterface("events")
package com.example.store.orders.events;
