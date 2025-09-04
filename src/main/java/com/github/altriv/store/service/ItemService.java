package com.github.altriv.store.service;

import com.github.altriv.store.model.ItemsPage;

public interface ItemService {

    ItemsPage findItems(int pageNumber, int pageSize);

    void saveItem(String title, String description, int price, byte[] image);

    void deleteItem(long itemId);

    byte[] getItemImage(long itemId);
}
