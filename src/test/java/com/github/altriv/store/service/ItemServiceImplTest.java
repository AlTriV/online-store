package com.github.altriv.store.service;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.mapper.ItemMapperImpl;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.repository.ItemRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ItemServiceImpl.class, ItemMapperImpl.class})
class ItemServiceImplTest {

    @MockitoBean
    private ItemRepository itemRepository;

    @Autowired
    private ItemServiceImpl itemService;

    @Nested
    class GetItemsPageTest {

        @ParameterizedTest
        @CsvSource({", NO", "'', "})
        void shouldThrowException(String search, ItemSorting sort) {
            assertThrows(NullPointerException.class, () -> itemService.getItemsPage(search, sort, 1, 10));
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

            PageInfo expectedPageInfo = new PageInfo(page, pageSize, false);

            when(itemRepository.findAll(eq(pageRequest)))
                    .thenReturn(new PageImpl<>(List.of(itemEntity1, itemEntity2), pageRequest, 2));

            ItemsPage itemsPage = itemService.getItemsPage(search, sort, pageNumber, pageSize);

            assertNotNull(itemsPage);
            List<Item> items = itemsPage.getItems();
            assertNotNull(items);
            assertEquals(2, items.size());
            assertTrue(items.containsAll(List.of(expectedItem1, expectedItem2)));
            PageInfo pageInfo = itemsPage.getPageInfo();
            assertNotNull(pageInfo);
            assertEquals(expectedPageInfo, pageInfo);
            verify(itemRepository, times(1)).findAll(eq(pageRequest));
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

            PageInfo expectedPageInfo = new PageInfo(page, pageSize, false);

            when(itemRepository.searchItemsWithTitleOrDescription(eq(search.toLowerCase()), eq(pageRequest)))
                    .thenReturn(new PageImpl<>(List.of(itemEntity1, itemEntity2), pageRequest, 2));

            ItemsPage itemsPage = itemService.getItemsPage(search, sort, pageNumber, pageSize);

            assertNotNull(itemsPage);
            List<Item> items = itemsPage.getItems();
            assertNotNull(items);
            assertEquals(2, items.size());
            assertTrue(items.containsAll(List.of(expectedItem1, expectedItem2)));
            PageInfo pageInfo = itemsPage.getPageInfo();
            assertNotNull(pageInfo);
            assertEquals(expectedPageInfo, pageInfo);
            verify(itemRepository, times(1))
                    .searchItemsWithTitleOrDescription(eq(search.toLowerCase()), eq(pageRequest));
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
}