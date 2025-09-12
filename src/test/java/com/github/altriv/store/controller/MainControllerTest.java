package com.github.altriv.store.controller;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.service.StoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MainController.class)
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @Test
    void startTest() throws Exception {
        String url = "/";

        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main/items"));
    }

    @Nested
    class GetItemsTest {

        @Test
        void shouldReturnItemPageIfDefaultParams() throws Exception {
            String expectedSearch = "";
            ItemSorting expectedSort = ItemSorting.NO;
            int expectedPageNumber = 1;
            int expectedPageSize = 10;
            Item item = new Item(1L, "title", "description", 1000, 0);
            Item item2 = new Item(2L, "title2", "description2", 2000, 0);
            ItemsPage itemsPage = new ItemsPage(
                    List.of(item, item2),
                    new PageInfo(expectedPageNumber, expectedPageSize, false)
            );
            when(storeService.searchItems(expectedSearch, expectedSort, expectedPageNumber, expectedPageSize)).thenReturn(Mono.just(itemsPage));

            String url = "/main/items";

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(view().name("main"))
                    .andExpect(model().attributeExists("paging"))
                    .andExpect(model().attribute("paging", itemsPage.getPageInfo()))
                    .andExpect(model().attributeExists("items"))
                    .andExpect(model().attribute("items", itemsPage.getItemRows(3)));
            verify(storeService, times(1)).searchItems(expectedSearch, expectedSort, expectedPageNumber, expectedPageSize);
        }

        @ParameterizedTest
        @CsvSource({
                "'', NO, 2, 10",
                "'tit', ALPHA, 2, 10",
                "'title', PRICE, 2, 10",
                "'', NO, 3, 5",
                "'test', ALPHA, 1, 50",
                "'', PRICE, 2, 5"
        })
        void shouldReturnItemPageIfSearchParams(String search, String sort, int pageNumber, int pageSize) throws Exception {
            ItemSorting itemSorting = ItemSorting.valueOf(sort);
            Item item = new Item(1L, "title", "description", 1000, 0);
            Item item2 = new Item(2L, "title2", "description2", 2000, 0);
            ItemsPage itemsPage = new ItemsPage(
                    List.of(item, item2),
                    new PageInfo(pageNumber, pageSize, false)
            );
            when(storeService.searchItems(search, itemSorting, pageNumber, pageSize)).thenReturn(Mono.just(itemsPage));

            String url = "/main/items";

            MockHttpServletRequestBuilder request = get(url)
                    .param("search", search)
                    .param("sort", sort)
                    .param("pageNumber", Integer.toString(pageNumber))
                    .param("pageSize", Integer.toString(pageSize));
            mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(view().name("main"))
                    .andExpect(model().attributeExists("paging"))
                    .andExpect(model().attribute("paging", itemsPage.getPageInfo()))
                    .andExpect(model().attributeExists("items"))
                    .andExpect(model().attribute("items", itemsPage.getItemRows(3)));
            verify(storeService, times(1)).searchItems(search, itemSorting, pageNumber, pageSize);
        }
    }

    @Nested
    class ChangeItemCountTest {

        @ParameterizedTest
        @ValueSource(strings = {"PLUS", "MINUS", "DELETE"})
        void shouldPerformActionAndReturnToMainPage(String action) throws Exception {
            long id = 1L;

            String url = "/main/items/" + id;

            when(storeService.changeItemCountInCart(id, ItemAction.valueOf(action))).thenReturn(Mono.empty());

            mockMvc.perform(multipart(url).param("action", action))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/main/items"));
            verify(storeService, times(1)).changeItemCountInCart(id, ItemAction.valueOf(action));
        }

        @ParameterizedTest
        @CsvSource({
                "'plus', 1",
                "'minus', 1",
                "'Delete', 1",
                "'DELETE', id"
        })
        void shouldReturnClientError(String action, String id) throws Exception {
            String url = "/main/items/" + id;

            mockMvc.perform(multipart(url).param("action", action))
                    .andExpect(status().is4xxClientError());
            verifyNoInteractions(storeService);
        }
    }

    @Nested
    class BuyItems {

        @Test
        void shouldRedirectToMainPage() throws Exception {
            String url = "/buy";

            when(storeService.buyItemsInCart()).thenReturn(Mono.empty());

            mockMvc.perform(post(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/main/items"));
            verify(storeService, times(1)).buyItemsInCart();
        }

        @Test
        void shouldReturnPaidOrderPage() throws Exception {
            String url = "/buy";
            long orderId = 1L;
            Order order = new Order(orderId, List.of());
            String expectedRedirectUrl = "/orders/" + orderId + "?newOrder=true";

            when(storeService.buyItemsInCart()).thenReturn(Mono.just(order));

            mockMvc.perform(post(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(expectedRedirectUrl));
            verify(storeService, times(1)).buyItemsInCart();
        }
    }

}