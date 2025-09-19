package com.github.altriv.store.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

public class Cart {

    private Long currentBalance;
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

    public int getTotalPrice() {
        int totalPrice = 0;
        for (Item item : items.values()) {
            totalPrice = totalPrice + item.getPrice() * item.getCount();
        }
        return totalPrice;
    }

    public Cart putBalance(Long balance) {
        this.currentBalance = balance;
        return this;
    }

    public boolean isPossibleToPayForCart() {
        if (isNull(currentBalance)) {
            return false;
        } else {
            return currentBalance >= getTotalPrice();
        }
    }

    public String getPaymentUnavailableReason() {
        if (isNull(currentBalance)) {
            return "Нет информации о балансе, возможность оплаты временно недоступна";
        } else if (currentBalance < getTotalPrice()) {
            return "Недостаточно средств для оплаты";
        } else {
            return null;
        }
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public static Cart empty() {
        return new Cart();
    }
}
