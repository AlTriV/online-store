package com.github.altriv.store.service;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.repository.ItemRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    public ItemsPage getItemsPage(@NonNull String search,
                                  @NonNull ItemSorting sort,
                                  int pageNumber,
                                  int pageSize) {
        int page = Math.max(0, pageNumber - 1);
        PageRequest pageRequest = PageRequest.of(page, pageSize, convertSort(sort));
        String searchString = search.toLowerCase();

        return itemRepository.findAllByTitleContainingOrDescriptionContaining(searchString, searchString, pageRequest)
                .collectList()
                .zipWith(itemRepository.countAllByTitleContainingOrDescriptionContaining(searchString, searchString))
                .map(p -> new PageImpl<>(p.getT1(), pageRequest, p.getT2()))
                .map(itemEntities -> {
                    List<Item> items = itemEntities.stream().map(this::toItem).toList();
                    PageInfo pageInfo = new PageInfo(page + 1, pageSize, itemEntities.hasNext());
                    return new ItemsPage(items, pageInfo);
                })
                .blockOptional()
                .orElse(new ItemsPage(List.of(), new PageInfo(page + 1, pageSize, false)));
    }

    @Override
    public Optional<Item> getItem(long itemId) {
        return itemRepository.findById(itemId)
                .map(this::toItem)
                .blockOptional();
    }

    @Override
    public byte[] getItemImage(long itemId) {
        return itemRepository.findById(itemId)
                .map(ItemEntity::getImage)
                .switchIfEmpty(Mono.just(new byte[0]))
                .block();
    }

    @Override
    public void saveItem(@NonNull String title, @NonNull String description, int price, byte[] image) {
        ItemEntity itemEntity = ItemEntity.builder()
                .title(title)
                .description(description)
                .price(price)
                .image(image)
                .build();
        itemRepository.save(itemEntity).subscribe();
    }

    @Override
    public void deleteItem(long itemId) {
        itemRepository.deleteById(itemId).subscribe();
    }

    private Sort convertSort(@NonNull ItemSorting sorting) {
        return switch (sorting) {
            case ALPHA -> Sort.by(Sort.Direction.ASC, "title");
            case PRICE -> Sort.by(Sort.Direction.ASC, "price");
            case NO -> Sort.unsorted();
        };
    }

    private Item toItem(@NonNull ItemEntity itemEntity) {
        return Item.builder()
                .id(itemEntity.getId())
                .title(itemEntity.getTitle())
                .description(itemEntity.getDescription())
                .price(itemEntity.getPrice())
                .count(0)
                .build();
    }
}
