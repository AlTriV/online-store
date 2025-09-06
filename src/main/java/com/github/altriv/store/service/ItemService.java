package com.github.altriv.store.service;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import lombok.NonNull;

import java.util.Optional;

public interface ItemService {

    ItemsPage getItemsPage(@NonNull String search, @NonNull ItemSorting sort, int pageNumber, int pageSize);

    Optional<Item> getItem(long itemId);

    byte[] getItemImage(long itemId);

    void saveItem(@NonNull String title, @NonNull String description, int price, byte[] image);

    void deleteItem(long itemId);
}
