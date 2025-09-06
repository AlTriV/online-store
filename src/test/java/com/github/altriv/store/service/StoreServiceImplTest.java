package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {

    @Mock
    private ItemService itemService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private StoreServiceImpl storeService;

    @Nested
    class SearchItems {

        @ParameterizedTest
        @CsvSource({", NO", "'', "})
        void shouldThrowNPEIfSearchStringOrSortingIsNull(String searchString, ItemSorting itemSorting) {
            assertThrows(
                    NullPointerException.class,
                    () -> storeService.searchItems(searchString, itemSorting, 1, 10)
            );
        }

        @Test
        void shouldReturnItemsPage() {
            String search = "item";
            ItemSorting sort = ItemSorting.NO;
            int pageNumber = 1;
            int pageSize = 10;

            Item cartItem1 = new Item(1L, "item1", "item1 description", 1200, 5);
            Item cartItem2 = new Item(2L, "item2", "item2 description", 1300, 3);
            Item item1 = new Item(1L, "item1", "item1 description", 1200, 0);
            Item item2 = new Item(2L, "item2", "item2 description", 1300, 0);
            Item item3 = new Item(3L, "item3", "item3 description", 1500, 0);
            Item item4 = new Item(4L, "item4", "item4 description", 1500, 0);

            Cart cart = new Cart(List.of(cartItem1, cartItem2));
            ItemsPage itemsPage = new ItemsPage(
                    List.of(item1, item2, item3, item4),
                    new PageInfo(pageNumber, pageSize,
                            false)
            );
            when(itemService.getItemsPage(search, sort, pageNumber, pageSize)).thenReturn(itemsPage);
            when(orderService.getNotPaidOrderAsCart()).thenReturn(cart);

            ItemsPage resultItemsPage = storeService.searchItems(search, sort, pageNumber, pageSize);

            assertNotNull(resultItemsPage);
            assertEquals(itemsPage.getPageInfo(), resultItemsPage.getPageInfo());
            assertEquals(4, resultItemsPage.getItems().size());
            assertTrue(resultItemsPage.getItems().containsAll(List.of(cartItem1, cartItem2, item3, item4)));
            verify(itemService, times(1)).getItemsPage(search, sort, pageNumber, pageSize);
            verify(orderService, times(1)).getNotPaidOrderAsCart();
        }
    }
}