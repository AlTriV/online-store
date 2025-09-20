package com.github.altriv.store.service;

import com.github.altriv.store.config.StoreCacheProperties;
import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.repository.ItemRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    private final String itemCachePrefix = "item:";

    private final String cacheTtl = "PT20S";

    @Mock
    private ReactiveRedisOperations<String, ItemEntity> itemRedisOperations;

    @Mock
    private ReactiveValueOperations<String, ItemEntity> itemValueOperations;

    @Mock
    private StoreCacheProperties cacheProperties;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Nested
    class GetItemsPageTest {

        @ParameterizedTest
        @CsvSource({", NO", "'', "})
        void shouldThrowException(String search, ItemSorting sort) {
            assertThrows(
                    NullPointerException.class,
                    () -> itemService.getItemsPage(search, sort, 1, 10).subscribe()
            );
        }

        @ParameterizedTest
        @CsvSource({
                "NO, 1, 5",
                "ALPHA, -1, 10",
                "PRICE, 2, 5"
        })
        void shouldFindAllIfSearchStringIsEmpty(ItemSorting sort, int pageNumber, int pageSize) {
            String search = "";
            int page = Math.max(0, pageNumber - 1);

            PageRequest pageRequest = PageRequest.of(page, pageSize, convertSort(sort));

            long id1 = 1L;
            String title1 = "Item 1";
            String description1 = "Description 1";
            int price1 = 1000;
            Item expectedItem1 = new Item(id1, title1, description1, price1, 0);
            ItemEntity itemEntity1 = new ItemEntity(id1, title1, description1, price1, null);

            long itemId2 = 2L;
            String itemTitle2 = "Item 2";
            String description2 = "Description 2";
            int price2 = 2000;
            Item expectedItem2 = new Item(itemId2, itemTitle2, description2, price2, 0);
            ItemEntity itemEntity2 = new ItemEntity(itemId2, itemTitle2, description2, price2, null);

            PageInfo expectedPageInfo = new PageInfo(page + 1, pageSize, false);

            when(itemRepository.findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageRequest))
                    .thenReturn(Flux.fromIterable(List.of(itemEntity1, itemEntity2)));
            when(itemRepository.countAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search))
                    .thenReturn(Mono.just(2L));

            itemService.getItemsPage(search, sort, pageNumber, pageSize)
                    .doOnNext(itemsPage -> {
                        assertNotNull(itemsPage);
                        List<Item> items = itemsPage.getItems();
                        assertNotNull(items);
                        assertEquals(2, items.size());
                        assertTrue(items.containsAll(List.of(expectedItem1, expectedItem2)));
                        PageInfo pageInfo = itemsPage.getPageInfo();
                        assertNotNull(pageInfo);
                        assertEquals(expectedPageInfo, pageInfo);
                    })
                    .subscribe();

            verify(itemRepository, times(1))
                    .findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq(search), eq(search), eq(pageRequest));
            verify(itemRepository, times(1))
                    .countAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq(search), eq(search));
            verifyNoMoreInteractions(itemRepository);
        }

        @ParameterizedTest
        @CsvSource({
                "NO, 1, 5",
                "ALPHA, -1, 10",
                "PRICE, 2, 5"
        })
        void shouldFindByTitleOrDescriptionIfSearchStringIsNotEmpty(ItemSorting sort, int pageNumber, int pageSize) {
            String search = "item";
            int page = Math.max(0, pageNumber - 1);

            PageRequest pageRequest = PageRequest.of(page, pageSize, convertSort(sort));

            long id1 = 1L;
            String title1 = "Item 1";
            String description1 = "Description 1";
            int price1 = 1000;
            Item expectedItem1 = new Item(id1, title1, description1, price1, 0);
            ItemEntity itemEntity1 = new ItemEntity(id1, title1, description1, price1, null);

            long itemId2 = 2L;
            String itemTitle2 = "Item 2";
            String description2 = "Description 2";
            int price2 = 2000;
            Item expectedItem2 = new Item(itemId2, itemTitle2, description2, price2, 0);
            ItemEntity itemEntity2 = new ItemEntity(itemId2, itemTitle2, description2, price2, null);

            PageInfo expectedPageInfo = new PageInfo(page + 1, pageSize, false);

            when(itemRepository.findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageRequest))
                    .thenReturn(Flux.fromIterable(List.of(itemEntity1, itemEntity2)));
            when(itemRepository.countAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search))
                    .thenReturn(Mono.just(2L));

            itemService.getItemsPage(search, sort, pageNumber, pageSize)
                    .doOnNext(itemsPage -> {
                        assertNotNull(itemsPage);
                        List<Item> items = itemsPage.getItems();
                        assertNotNull(items);
                        assertEquals(2, items.size());
                        assertTrue(items.containsAll(List.of(expectedItem1, expectedItem2)));
                        PageInfo pageInfo = itemsPage.getPageInfo();
                        assertNotNull(pageInfo);
                        assertEquals(expectedPageInfo, pageInfo);
                    })
                    .subscribe();

            verify(itemRepository, times(1))
                    .findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq(search), eq(search), eq(pageRequest));
            verify(itemRepository, times(1))
                    .countAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq(search), eq(search));
            verifyNoMoreInteractions(itemRepository);
        }

        private Sort convertSort(ItemSorting sorting) {
            return switch (sorting) {
                case ALPHA -> Sort.by(Sort.Direction.ASC, "title");
                case PRICE -> Sort.by(Sort.Direction.ASC, "price");
                case NO -> Sort.unsorted();
            };
        }
    }

    @Nested
    class GetItem {

        @BeforeEach
        void setUp() {
            when(cacheProperties.ttl()).thenReturn(cacheTtl);
            when(cacheProperties.itemCachePrefix()).thenReturn(itemCachePrefix);
            when(itemRedisOperations.opsForValue()).thenReturn(itemValueOperations);
        }

        @Test
        void shouldReturnEmptyWhenNoItemFound() {
            long itemId = 1L;
            String itemCacheKey = cacheProperties.itemCachePrefix() + itemId;
            when(itemValueOperations.get(eq(itemCacheKey))).thenReturn(Mono.empty());
            when(itemRepository.findById(itemId)).thenReturn(Mono.empty());

            itemService.getItem(itemId)
                    .doOnNext(Assertions::assertNull)
                    .subscribe();

            verify(itemValueOperations, times(1)).get(eq(itemCacheKey));
            verify(itemRepository, times(1)).findById(itemId);
        }

        @Test
        void shouldReturnItemFromDatabaseAndSaveToCache() {
            long itemId = 1L;
            String itemTitle = "Item 1";
            String itemDescription = "Description 1";
            int itemPrice = 2000;
            ItemEntity itemEntity = new ItemEntity(itemId, itemTitle, itemDescription, itemPrice, null);
            Item expectedItem = new Item(itemId, itemTitle, itemDescription, itemPrice, 0);

            Duration ttl = Duration.parse(cacheProperties.ttl());
            String itemCacheKey = cacheProperties.itemCachePrefix() + itemId;
            Mono<Boolean> saveToCacheFlag = spy(Mono.just(true));
            when(itemValueOperations.get(eq(itemCacheKey))).thenReturn(Mono.empty());
            when(itemRepository.findById(itemId)).thenReturn(Mono.just(itemEntity));
            when(itemValueOperations.set(eq(itemCacheKey), eq(itemEntity), eq(ttl))).thenReturn(saveToCacheFlag);
            when(saveToCacheFlag.thenReturn(itemEntity)).thenReturn(Mono.just(itemEntity));

            itemService.getItem(itemId)
                    .doOnNext(Assertions::assertNotNull)
                    .doOnNext(resultEntity -> assertEquals(expectedItem, resultEntity))
                    .subscribe();

            verify(itemValueOperations, times(1)).get(eq(itemCacheKey));
            verify(itemRepository, times(1)).findById(itemId);
            verify(itemValueOperations, times(1)).set(eq(itemCacheKey), eq(itemEntity), eq(ttl));
        }

        @Test
        void shouldReturnItemFromCache() {
            long itemId = 1L;
            String itemTitle = "Item 1";
            String itemDescription = "Description 1";
            int itemPrice = 2000;
            ItemEntity itemEntity = new ItemEntity(itemId, itemTitle, itemDescription, itemPrice, null);
            Item expectedItem = new Item(itemId, itemTitle, itemDescription, itemPrice, 0);

            String itemCacheKey = cacheProperties.itemCachePrefix() + itemId;
            when(itemValueOperations.get(eq(itemCacheKey))).thenReturn(Mono.just(itemEntity));
            when(itemRepository.findById(itemId)).thenReturn(Mono.empty());

            itemService.getItem(itemId)
                    .doOnNext(Assertions::assertNotNull)
                    .doOnNext(resultEntity -> assertEquals(expectedItem, resultEntity))
                    .subscribe();

            verify(itemValueOperations, times(1)).get(eq(itemCacheKey));
            verify(itemRepository, times(1)).findById(itemId);
            verifyNoMoreInteractions(itemValueOperations);
        }
    }

    @Nested
    class GetItemImage {

        @BeforeEach
        void setUp() {
            when(cacheProperties.ttl()).thenReturn(cacheTtl);
            when(cacheProperties.itemCachePrefix()).thenReturn(itemCachePrefix);
            when(itemRedisOperations.opsForValue()).thenReturn(itemValueOperations);
        }

        @Test
        void shouldReturnEmptyWhenNoItemFound() {
            long itemId = 1L;
            String itemCacheKey = cacheProperties.itemCachePrefix() + itemId;
            when(itemValueOperations.get(eq(itemCacheKey))).thenReturn(Mono.empty());
            when(itemRepository.findById(itemId)).thenReturn(Mono.empty());

            itemService.getItemImage(itemId)
                    .doOnNext(itemImage -> assertArrayEquals(new byte[0], itemImage))
                    .subscribe();

            verify(itemValueOperations, times(1)).get(eq(itemCacheKey));
            verify(itemRepository, times(1)).findById(itemId);
        }

        @Test
        void shouldReturnItemImageWhenItemFoundInDatabaseAndSaveItemToCache() {
            long itemId = 1L;
            byte[] image = "dummy image content".getBytes();
            ItemEntity itemEntity = new ItemEntity(itemId, "title", "description", 1000, image);

            Duration ttl = Duration.parse(cacheProperties.ttl());
            String itemCacheKey = cacheProperties.itemCachePrefix() + itemId;
            Mono<Boolean> saveToCacheFlag = spy(Mono.just(true));
            when(itemValueOperations.get(eq(itemCacheKey))).thenReturn(Mono.empty());
            when(itemRepository.findById(itemId)).thenReturn(Mono.just(itemEntity));
            when(itemValueOperations.set(eq(itemCacheKey), eq(itemEntity), eq(ttl))).thenReturn(saveToCacheFlag);
            when(saveToCacheFlag.thenReturn(itemEntity)).thenReturn(Mono.just(itemEntity));

            itemService.getItemImage(itemId)
                    .doOnNext(itemImage -> assertArrayEquals(image, itemImage))
                    .subscribe();

            verify(itemValueOperations, times(1)).get(eq(itemCacheKey));
            verify(itemRepository, times(1)).findById(itemId);
            verify(itemValueOperations, times(1)).set(eq(itemCacheKey), eq(itemEntity), eq(ttl));
        }

        @Test
        void shouldReturnItemImageWhenItemFoundInCache() {
            long itemId = 1L;
            byte[] image = "dummy image content".getBytes();
            ItemEntity itemEntity = new ItemEntity(itemId, "title", "description", 1000, image);

            String itemCacheKey = cacheProperties.itemCachePrefix() + itemId;
            when(itemValueOperations.get(eq(itemCacheKey))).thenReturn(Mono.just(itemEntity));
            when(itemRepository.findById(itemId)).thenReturn(Mono.empty());

            itemService.getItemImage(itemId)
                    .doOnNext(itemImage -> assertArrayEquals(image, itemImage))
                    .subscribe();

            verify(itemValueOperations, times(1)).get(eq(itemCacheKey));
            verify(itemRepository, times(1)).findById(itemId);
            verifyNoMoreInteractions(itemValueOperations);
        }
    }
}