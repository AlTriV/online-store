package com.github.altriv.store.service;

import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import lombok.NonNull;

public interface StoreService {

    ItemsPage searchItems(@NonNull String search, @NonNull ItemSorting sort, int pageNumber, int pageSize);

    void changeItemCountInCart(long itemId, @NonNull ItemAction action);
}
