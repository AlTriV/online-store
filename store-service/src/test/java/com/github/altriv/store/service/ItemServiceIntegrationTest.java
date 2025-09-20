package com.github.altriv.store.service;

import com.github.altriv.paymentclient.PaymentClient;
import com.github.altriv.store.config.StoreCacheProperties;
import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.repository.ItemRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockitoBean(types = PaymentClient.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=com.github.altriv.paymentclient.PaymentClientAutoConfiguration",
        "store.cache.itemCachePrefix='item:'",
        "store.cache.orderCachePrefix='order:'",
        "store.cache.ttl=PT3S"
})
public class ItemServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17:5");

    @Container
    static RedisContainer redis = new RedisContainer("redis:8.2.1-bookworm");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.r2dbc.url",
                () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName()
        );
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReactiveRedisOperations<String, ItemEntity> itemReactiveOperations;

    @Autowired
    private StoreCacheProperties storeCacheProperties;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll().block();
        itemReactiveOperations.keys(storeCacheProperties.itemCachePrefix() + "*")
                .flatMap(itemReactiveOperations.opsForValue()::delete)
                .blockLast();
    }

    @Test
    void shouldSaveNewItem() {
        String title = "New Item";
        String description = "New Description";
        int price = 1000;
        byte[] image = "dummy_image_content".getBytes();

        itemService.saveItem(title, description, price, image).block();

        itemRepository.findAll()
                .collectList()
                .doOnNext(itemEntities -> {
                    assertNotNull(itemEntities);
                    assertEquals(1, itemEntities.size());
                    ItemEntity itemEntity = itemEntities.getFirst();
                    assertEquals(title, itemEntity.getTitle());
                    assertEquals(description, itemEntity.getDescription());
                    assertEquals(price, itemEntity.getPrice());
                    assertArrayEquals(image, itemEntity.getImage());
                })
                .block();
    }

    @Test
    void shouldDeleteItem() {
        String title = "New Item";
        String description = "New Description";
        int price = 1000;
        byte[] image = "dummy_image_content".getBytes();
        ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);

        itemRepository.save(itemEntity)
                .flatMap(saved ->
                        itemService.deleteItem(saved.getId())
                                .then(itemRepository.existsById(saved.getId()))
                )
                .doOnNext(Assertions::assertFalse)
                .block();
    }

    @Nested
    class ItemImageTest {

        @Test
        void shouldReturnEmptyArrayIfItemIsNotExist() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] dummyImage = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, dummyImage);

            itemRepository.save(itemEntity)
                    .map(saved -> saved.getId() + 1L)
                    .flatMap(savedId -> itemService.getItemImage(savedId))
                    .doOnNext(image -> assertArrayEquals(new byte[0], image))
                    .block();
        }

        @Test
        void shouldReturnImageIfItemExistInDatabaseAndPutItemInCache() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);

            ItemEntity savedItem = itemRepository.save(itemEntity).block();
            Long itemId = savedItem.getId();

            itemService.getItemImage(itemId)
                    .doOnNext(foundImage -> assertArrayEquals(image, foundImage))
                    .block();
            itemReactiveOperations.hasKey(storeCacheProperties.itemCachePrefix() + itemId)
                    .doOnNext(Assertions::assertTrue)
                    .block();
        }

        @Test
        void shouldReturnImageIfItemExistInCache() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            long itemId = 1L;
            ItemEntity itemEntity = new ItemEntity(itemId, title, description, price, image);

            itemReactiveOperations.opsForValue().set(storeCacheProperties.itemCachePrefix() + itemEntity.getId(), itemEntity).block();

            itemService.getItemImage(itemId)
                    .doOnNext(foundImage -> assertArrayEquals(image, foundImage))
                    .block();
        }
    }

    @Nested
    class GetItemTest {

        @Test
        void shouldReturnItemFromDBAndPutItemInCache() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);

            itemRepository.save(itemEntity)
                    .flatMap(saved ->
                            itemService.getItem(saved.getId())
                                    .doOnNext(item -> {
                                        assertEquals(saved.getId(), item.getId());
                                        assertEquals(title, item.getTitle());
                                        assertEquals(description, item.getDescription());
                                        assertEquals(price, item.getPrice());
                                        assertEquals(0, item.getCount());
                                    }))
                    .map(Item::getId)
                    .flatMap(itemId -> itemReactiveOperations.hasKey(storeCacheProperties.itemCachePrefix() + itemId).doOnNext(Assertions::assertTrue))
                    .block();
        }

        @Test
        void shouldReturnItemCache() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            long itemId = 1L;
            ItemEntity itemEntity = new ItemEntity(itemId, title, description, price, image);

            itemReactiveOperations.opsForValue().set(storeCacheProperties.itemCachePrefix() + itemEntity.getId(), itemEntity).block();

            itemService.getItem(itemId)
                    .doOnNext(item -> {
                        assertEquals(itemId, item.getId());
                        assertEquals(title, item.getTitle());
                        assertEquals(description, item.getDescription());
                        assertEquals(price, item.getPrice());
                        assertEquals(0, item.getCount());
                    })
                    .block();
        }

        @Test
        void shouldReturnEmptyIfItemIsNotExist() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);

            itemRepository.save(itemEntity)
                    .flatMap(saved ->
                            itemService.getItem(saved.getId() + 1L)
                                    .doOnNext(Assertions::assertNull))
                    .block();
        }
    }

    @Nested
    class GetItemPageTest {

        @ParameterizedTest
        @EnumSource(ItemSorting.class)
        void shouldReturnFirstPageSearchStringIsEmpty(ItemSorting sorting) {
            int page = 1;
            int pageSize = 2;
            ItemEntity itemEntity1 = new ItemEntity(null, "title2", "description2", 300, new byte[0]);
            ItemEntity itemEntity2 = new ItemEntity(null, "title3", "description3", 100, new byte[0]);
            ItemEntity itemEntity3 = new ItemEntity(null, "title1", "description1", 200, new byte[0]);
            List<ItemEntity> itemEntities = List.of(itemEntity1, itemEntity2, itemEntity3);

            itemRepository.saveAll(itemEntities).collectList().block();

            List<Item> expectedSortedItems = expectedSortedItems(itemEntities, sorting);

            itemService.getItemsPage("", sorting, page, pageSize)
                    .doOnNext(itemsPage -> {
                        assertNotNull(itemsPage);
                        List<Item> items = itemsPage.getItems();
                        assertEquals(2, items.size());
                        assertEquals(expectedSortedItems.get(0).getTitle(), items.get(0).getTitle());
                        assertEquals(expectedSortedItems.get(1).getTitle(), items.get(1).getTitle());
                        PageInfo pageInfo = itemsPage.getPageInfo();
                        assertNotNull(pageInfo);
                        assertEquals(page, pageInfo.pageNumber());
                        assertEquals(pageSize, pageInfo.pageSize());
                        assertTrue(pageInfo.hasNext());
                    })
                    .block();
        }

        @ParameterizedTest
        @EnumSource(ItemSorting.class)
        void shouldReturnSecondPageSearchStringIsEmpty(ItemSorting sorting) {
            int page = 2;
            int pageSize = 2;
            ItemEntity itemEntity1 = new ItemEntity(null, "title2", "description2", 300, new byte[0]);
            ItemEntity itemEntity2 = new ItemEntity(null, "title3", "description3", 100, new byte[0]);
            ItemEntity itemEntity3 = new ItemEntity(null, "title1", "description1", 200, new byte[0]);
            List<ItemEntity> itemEntities = List.of(itemEntity1, itemEntity2, itemEntity3);

            itemRepository.saveAll(itemEntities).collectList().block();

            List<Item> expectedSortedItems = expectedSortedItems(itemEntities, sorting);

            itemService.getItemsPage("", sorting, page, pageSize)
                    .doOnNext(itemsPage -> {
                        assertNotNull(itemsPage);
                        List<Item> items = itemsPage.getItems();
                        assertEquals(1, items.size());
                        assertEquals(expectedSortedItems.get(2).getTitle(), items.getFirst().getTitle());
                        PageInfo pageInfo = itemsPage.getPageInfo();
                        assertNotNull(pageInfo);
                        assertEquals(page, pageInfo.pageNumber());
                        assertEquals(pageSize, pageInfo.pageSize());
                        assertFalse(pageInfo.hasNext());
                    })
                    .block();
        }

        @ParameterizedTest
        @EnumSource(ItemSorting.class)
        void shouldReturnSpecificItemsIfSearchStringIsNotEmpty(ItemSorting sorting) {
            int page = 1;
            int pageSize = 2;
            ItemEntity itemEntity1 = new ItemEntity(null, "title2", "description2", 300, new byte[0]);
            ItemEntity itemEntity2 = new ItemEntity(null, "title_seaRch3", "description3", 100, new byte[0]);
            ItemEntity itemEntity3 = new ItemEntity(null, "title1", "description_1", 200, new byte[0]);
            ItemEntity itemEntity4 = new ItemEntity(null, "title", "description4_search", 500, new byte[0]);
            List<ItemEntity> itemEntities = List.of(itemEntity1, itemEntity2, itemEntity3, itemEntity4);

            itemRepository.saveAll(itemEntities).collectList().block();

            List<Item> expectedSortedItems = expectedSortedItems(List.of(itemEntity2, itemEntity4), sorting);

            itemService.getItemsPage("SEARCH", sorting, page, pageSize)
                    .doOnNext(itemsPage -> {
                        assertNotNull(itemsPage);
                        List<Item> items = itemsPage.getItems();
                        assertEquals(2, items.size());
                        assertEquals(expectedSortedItems.get(0).getTitle(), items.get(0).getTitle());
                        assertEquals(expectedSortedItems.get(1).getTitle(), items.get(1).getTitle());
                        PageInfo pageInfo = itemsPage.getPageInfo();
                        assertNotNull(pageInfo);
                        assertEquals(page, pageInfo.pageNumber());
                        assertEquals(pageSize, pageInfo.pageSize());
                        assertFalse(pageInfo.hasNext());
                    })
                    .block();
        }

        private List<Item> expectedSortedItems(List<ItemEntity> itemEntities, ItemSorting sorting) {
            List<Item> itemList = itemEntities.stream()
                    .map(e -> new Item(e.getId(), e.getTitle(), e.getDescription(), e.getPrice(), 0))
                    .toList();
            List<Item> sortedItems = new ArrayList<>(itemList);
            return switch (sorting) {
                case NO -> sortedItems;
                case ALPHA -> {
                    sortedItems.sort(Comparator.comparing(Item::getTitle));
                    yield sortedItems;
                }
                case PRICE -> {
                    sortedItems.sort(Comparator.comparing(Item::getPrice));
                    yield sortedItems;
                }
            };
        }
    }
}
