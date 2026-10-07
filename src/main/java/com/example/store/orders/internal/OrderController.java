package com.example.store.orders.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.inventory.InsufficientStockException;
import com.example.store.orders.OrderManagement;
import com.example.store.orders.OrderSummary;

@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderManagement orders;

    OrderController(OrderManagement orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> place(@RequestBody PlaceOrderRequest request) {
        UUID orderId = orders.place(request.sku(), request.quantity(), request.customerEmail());
        return Map.of("orderId", orderId);
    }

    @GetMapping
    List<OrderSummary> all() {
        return orders.findAll();
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> insufficientStock(InsufficientStockException exception) {
        return Map.of("error", exception.getMessage());
    }
}
