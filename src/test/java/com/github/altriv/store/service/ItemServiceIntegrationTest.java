package com.github.altriv.store.service;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class ItemServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:17:5");
    }

    @DynamicPropertySource
    static void registerPostgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
    }

    @Test
    void shouldSaveNewItem() {
        String title = "New Item";
        String description = "New Description";
        int price = 1000;
        byte[] image = "dummy_image_content".getBytes();

        itemService.saveItem(title, description, price, image);

        List<ItemEntity> itemEntities = itemRepository.findAll();

        assertNotNull(itemEntities);
        assertEquals(1, itemEntities.size());
        ItemEntity itemEntity = itemEntities.getFirst();
        assertEquals(title, itemEntity.getTitle());
        assertEquals(description, itemEntity.getDescription());
        assertEquals(price, itemEntity.getPrice());
        assertArrayEquals(image, itemEntity.getImage());
    }

    @Test
    void shouldDeleteItem() {
        String title = "New Item";
        String description = "New Description";
        int price = 1000;
        byte[] image = "dummy_image_content".getBytes();
        ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);
        ItemEntity saved = itemRepository.save(itemEntity);

        itemService.deleteItem(saved.getId());
        Optional<ItemEntity> deletedItemEntity = itemRepository.findById(saved.getId());

        assertFalse(deletedItemEntity.isPresent());
    }

    @Nested
    class ItemImageTest {

        @Test
        void shouldReturnEmptyArrayIfItemIsNotExist() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);
            ItemEntity saved = itemRepository.save(itemEntity);

            long notExistsImageId = saved.getId() + 1L;

            byte[] itemImage = itemService.getItemImage(notExistsImageId);

            assertArrayEquals(new byte[0], itemImage);
        }

        @Test
        void shouldReturnImageIfItemExist() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);
            ItemEntity saved = itemRepository.save(itemEntity);

            byte[] itemImage = itemService.getItemImage(saved.getId());

            assertArrayEquals(image, itemImage);
        }
    }

    @Nested
    class GetItemTest {

        @Test
        void shouldReturnItem() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);
            ItemEntity saved = itemRepository.save(itemEntity);

            Optional<Item> item = itemService.getItem(saved.getId());

            assertTrue(item.isPresent());
            assertEquals(saved.getId(), item.get().getId());
            assertEquals(title, item.get().getTitle());
            assertEquals(description, item.get().getDescription());
            assertEquals(price, item.get().getPrice());
            assertEquals(0, item.get().getCount());
        }

        @Test
        void shouldReturnEmptyIfItemIsNotExist() {
            String title = "New Item";
            String description = "New Description";
            int price = 1000;
            byte[] image = "dummy_image_content".getBytes();
            ItemEntity itemEntity = new ItemEntity(null, title, description, price, image);
            ItemEntity saved = itemRepository.save(itemEntity);

            long notExistsImageId = saved.getId() + 1L;

            Optional<Item> item = itemService.getItem(notExistsImageId);

            assertTrue(item.isEmpty());
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
            itemRepository.saveAll(itemEntities);

            List<Item> expectedSortedItems = expectedSortedItems(itemEntities, sorting);

            ItemsPage itemsPage = itemService.getItemsPage("", sorting, page, pageSize);

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
            itemRepository.saveAll(itemEntities);

            List<Item> expectedSortedItems = expectedSortedItems(itemEntities, sorting);

            ItemsPage itemsPage = itemService.getItemsPage("", sorting, page, pageSize);

            assertNotNull(itemsPage);
            List<Item> items = itemsPage.getItems();
            assertEquals(1, items.size());
            assertEquals(expectedSortedItems.get(2).getTitle(), items.getFirst().getTitle());
            PageInfo pageInfo = itemsPage.getPageInfo();
            assertNotNull(pageInfo);
            assertEquals(page, pageInfo.pageNumber());
            assertEquals(pageSize, pageInfo.pageSize());
            assertFalse(pageInfo.hasNext());
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
            itemRepository.saveAll(itemEntities);

            List<Item> expectedSortedItems = expectedSortedItems(List.of(itemEntity2, itemEntity4), sorting);

            ItemsPage itemsPage = itemService.getItemsPage("SEARCH", sorting, page, pageSize);

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
