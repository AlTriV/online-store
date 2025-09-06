package com.github.altriv.store.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

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

    public void removeOneUnit(long itemId) {
        ofNullable(this.items.get(itemId))
                .ifPresent(item -> {
                    item.decreaseCount();
                    if (item.getCount() == 0) {
                        this.items.remove(itemId);
                    }
                });
    }

    public void removeAllUnits(long itemId) {
        this.items.remove(itemId);
    }

    public List<Item> getItems() {
        return new ArrayList<>(items.values());
    }

    public Item getItem(long id) {
        return items.get(id);
    }

    public void changeItemCountInCart(Item item, ItemAction action) {
        long itemId = item.getId();
        switch (action) {
            case PLUS -> addItem(item);
            case MINUS -> removeOneUnit(itemId);
            case DELETE -> removeAllUnits(itemId);
        }
    }

    public static Cart empty() {
        return new Cart();
    }
}
