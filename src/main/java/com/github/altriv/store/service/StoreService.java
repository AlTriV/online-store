package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.Order;
import lombok.NonNull;

import java.util.Optional;

public interface StoreService {

    ItemsPage searchItems(@NonNull String search, @NonNull ItemSorting sort, int pageNumber, int pageSize);

    void changeItemCountInCart(long itemId, @NonNull ItemAction action);

    @NonNull
    Cart getCart();

    Optional<Item> getItemWithCartCount(long itemId);

    Optional<Order> buyItemsInCart();
}
