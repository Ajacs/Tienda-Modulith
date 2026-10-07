package com.example.store.orders.internal;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio (DDD) del agregado {@link Order}.
 * Tiene que ser public para que OrderManagement (otro paquete del mismo módulo) lo use.
 * Para el compilador de Java eso lo deja visible para toda la aplicación.
 * Spring Modulith es quien impide que otro módulo lo toque.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {}
