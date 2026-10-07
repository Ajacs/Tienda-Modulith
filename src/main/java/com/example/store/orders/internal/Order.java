package com.example.store.orders.internal;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Agregado (DDD) "Pedido": es la única puerta de entrada para cambiar su propio estado.
 * Hoy es un dato simple, pero las invariantes del pedido (por ejemplo, cantidades válidas)
 * vivirían como métodos aquí, no en {@code OrderManagement}.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String sku;
    private int quantity;
    private String customerEmail;

    protected Order() {
        // requerido por JPA
    }

    public Order(String sku, int quantity, String customerEmail) {
        this.sku = sku;
        this.quantity = quantity;
        this.customerEmail = customerEmail;
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }
}
