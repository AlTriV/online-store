package com.github.altriv.store.mapper;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Item;
import org.springframework.stereotype.Component;

@Component
public class ItemMapperImpl implements ItemMapper {

    @Override
    public Item toItem(ItemEntity itemEntity) {
        return Item.builder()
                .id(itemEntity.getId())
                .title(itemEntity.getTitle())
                .description(itemEntity.getDescription())
                .price(itemEntity.getPrice())
                .count(0)
                .build();
    }

    @Override
    public ItemEntity toItemEntity(Item item) {
        return ItemEntity.builder()
                .id(item.getId() == 0L ? null : item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .build();
    }
}
