/**
 * Módulo de pedidos.
 *
 * Lo que está en este paquete es la API del módulo. Lo que está en "internal" no puede
 * usarse desde otros módulos. "events" es una API con nombre (ver events/package-info.java).
 * Solo se le permite depender del módulo de inventario.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Pedidos", allowedDependencies = "inventory")
package com.example.store.orders;
