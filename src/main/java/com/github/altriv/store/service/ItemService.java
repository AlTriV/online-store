package com.github.altriv.store.service;

import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import lombok.NonNull;

public interface ItemService {

    ItemsPage getItemsPage(@NonNull String search, @NonNull ItemSorting sort, int pageNumber, int pageSize);

    void saveItem(@NonNull String title, @NonNull String description, int price, byte[] image);

    void deleteItem(long itemId);

    byte[] getItemImage(long itemId);
}
