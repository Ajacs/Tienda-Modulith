package com.example.store.orders.internal;

record PlaceOrderRequest(String sku, int quantity, String customerEmail) {}
