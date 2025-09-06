package com.github.altriv.store.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cart {

    private final Map<Long, Item> items;

    private Cart() {
        items = new HashMap<>();
    }

    public Cart(List<Item> items) {
        this.items = new HashMap<>();
        items.forEach(item -> this.items.put(item.getId(), item));
    }

    public void addItem(Item item) {
        long itemId = item.getId();
        if (this.items.containsKey(itemId)) {
            this.items.get(itemId).increaseCount();
        } else {
            item.setCount(1);
            this.items.put(itemId, item);
        }
    }

    public List<Item> getItems() {
        return new ArrayList<>(items.values());
    }

    public Item getItem(long id) {
        return items.get(id);
    }

    public static Cart empty() {
        return new Cart();
    }
}
