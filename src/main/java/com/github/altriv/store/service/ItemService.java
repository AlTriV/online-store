package com.github.altriv.store.service;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ItemService {

    Mono<ItemsPage> getItemsPage(@NonNull String search, @NonNull ItemSorting sort, int pageNumber, int pageSize);

    Mono<Item> getItem(long itemId);

    Mono<byte[]> getItemImage(long itemId);

    Mono<Void> saveItem(@NonNull String title, @NonNull String description, int price, byte[] image);

    Mono<Void> deleteItem(long itemId);
}
