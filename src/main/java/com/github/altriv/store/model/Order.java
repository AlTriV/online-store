package com.github.altriv.store.model;

import java.util.List;

public record Order(long id, List<Item> items) {

    public int totalPrice() {
        int totalPrice = 0;
        for (Item item : items) {
            totalPrice = totalPrice + item.getPrice() * item.getCount();
        }
        return totalPrice;
    }
}
