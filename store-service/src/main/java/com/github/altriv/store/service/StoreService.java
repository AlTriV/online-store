package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.Purchase;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface StoreService {

    Mono<ItemsPage> searchItems(@NonNull String search, @NonNull ItemSorting sort, int pageNumber, int pageSize);

    Mono<Void> changeItemCountInCart(long itemId, @NonNull ItemAction action);

    Mono<Cart> getCart();

    Mono<Item> getItemWithCartCount(long itemId);

    Mono<Purchase> buyItemsInCart();
}
