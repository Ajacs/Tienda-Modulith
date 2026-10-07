/**
 * Módulo de notificaciones.
 *
 * No expone nada: todas sus clases son package-private. Solo reacciona a eventos de pedidos,
 * y por eso solo necesita la API con nombre "events" de "orders" (ni ve OrderManagement
 * ni OrderSummary, solo OrderPlaced).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Notificaciones", allowedDependencies = "orders::events")
package com.example.store.notifications;
