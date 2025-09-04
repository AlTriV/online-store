package com.github.altriv.store.mapper;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Item;

public interface ItemMapper {

    Item toItem(ItemEntity itemEntity);

    ItemEntity toItemEntity(Item item);
}
