package com.github.altriv.store.service;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.mapper.ItemMapper;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Override
    public ItemsPage findItems(int pageNumber, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNumber - 1, pageSize);
        Page<ItemEntity> itemEntityPage = itemRepository.findAll(pageRequest);
        PageInfo pageInfo = new PageInfo(pageNumber, pageSize, itemEntityPage.hasNext());
        List<Item> items = itemEntityPage.stream()
                .map(itemMapper::toItem)
                .toList();
        return new ItemsPage(items, pageInfo);
    }

    @Override
    public void saveItem(String title, String description, int price, byte[] image) {
        ItemEntity itemEntity = ItemEntity.builder()
                .title(title)
                .description(description)
                .price(price)
                .image(image)
                .build();
        itemRepository.save(itemEntity);
    }

    @Override
    public void deleteItem(long itemId) {
        itemRepository.deleteById(itemId);
    }

    @Override
    public byte[] getItemImage(long itemId) {
        return itemRepository.getItemImage(itemId).orElse(new byte[0]);
    }
}
