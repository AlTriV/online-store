package com.github.altriv.store.service;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.repository.ItemRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private static final String ITEM_CACHE_PREFIX = "item:";

    @Value("${store.cache.ttl}")
    private String ttl = "PT20S";

    private final ItemRepository itemRepository;
    private final ReactiveRedisOperations<String, ItemEntity> itemRedisOperations;

    @Override
    public Mono<ItemsPage> getItemsPage(@NonNull String search,
                                        @NonNull ItemSorting sort,
                                        int pageNumber,
                                        int pageSize) {
        int page = Math.max(0, pageNumber - 1);
        PageRequest pageRequest = PageRequest.of(page, pageSize, convertSort(sort));
        String searchString = search.toLowerCase();

        return itemRepository.findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchString, searchString, pageRequest)
                .collectList()
                .zipWith(itemRepository.countAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchString, searchString))
                .map(p -> new PageImpl<>(p.getT1(), pageRequest, p.getT2()))
                .map(itemEntityPage -> {
                    List<Item> items = itemEntityPage.stream().map(this::toItem).toList();
                    PageInfo pageInfo = new PageInfo(page + 1, pageSize, itemEntityPage.hasNext());
                    return new ItemsPage(items, pageInfo);
                });
    }

    @Override
    public Mono<Item> getItem(long itemId) {
        return getItemEntity(itemId).map(this::toItem);
    }

    @Override
    public Mono<byte[]> getItemImage(long itemId) {
        return getItemEntity(itemId).map(ItemEntity::getImage).switchIfEmpty(Mono.just(new byte[0]));
    }

    private Mono<ItemEntity> getItemEntity(long itemId) {
        String itemCacheKey = ITEM_CACHE_PREFIX + itemId;
        Duration ttlDuration = Duration.parse(ttl);
        return itemRedisOperations.opsForValue().get(itemCacheKey)
                .switchIfEmpty(itemRepository.findById(itemId)
                        .flatMap(itemEntity -> itemRedisOperations.opsForValue().set(itemCacheKey, itemEntity, ttlDuration).thenReturn(itemEntity)));
    }

    @Override
    public Mono<Void> saveItem(@NonNull String title, @NonNull String description, int price, byte[] image) {
        ItemEntity itemEntity = ItemEntity.builder()
                .title(title)
                .description(description)
                .price(price)
                .image(image)
                .build();
        return itemRepository.save(itemEntity).then();
    }

    @Override
    public Mono<Void> deleteItem(long itemId) {
        return itemRepository.deleteById(itemId).then();
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
