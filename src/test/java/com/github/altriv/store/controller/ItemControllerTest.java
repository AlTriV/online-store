package com.github.altriv.store.controller;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.service.ItemService;
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
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private ItemService itemService;

    @Nested
    class GetItem {

        @Test
        void shouldRedirectToMainPageIfItemNotFound() throws Exception {
            long itemId = 1L;
            String url = "/items/" + itemId;

            when(storeService.getItemWithCartCount(itemId)).thenReturn(Mono.empty());

            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/main/items"));
            verify(storeService, times(1)).getItemWithCartCount(itemId);
        }

        @Test
        void shouldReturnItemPage() throws Exception {
            long itemId = 1L;
            String url = "/items/" + itemId;
            Item item = new Item(itemId, "title1", "item1 description", 1200, 5);

            when(storeService.getItemWithCartCount(itemId)).thenReturn(Mono.just(item));

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(view().name("item"))
                    .andExpect(model().attributeExists("item"))
                    .andExpect(model().attribute("item", item));
            verify(storeService, times(1)).getItemWithCartCount(itemId);
        }
    }

    @Nested
    class ChangeItemCountTest {

        @ParameterizedTest
        @ValueSource(strings = {"PLUS", "MINUS", "DELETE"})
        void shouldPerformActionAndReturnToMainPage(String action) throws Exception {
            long id = 1L;

            when(storeService.changeItemCountInCart(id, ItemAction.valueOf(action))).thenReturn(Mono.empty());

            String url = "/items/" + id;

            mockMvc.perform(multipart(url).param("action", action))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(url));
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
            String url = "/items/" + id;

            mockMvc.perform(multipart(url).param("action", action))
                    .andExpect(status().is4xxClientError());
            verifyNoInteractions(storeService);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "dummy_image_content"})
    void shouldReturnBytesOfImage(String dummyImageContent) throws Exception {
        byte[] imageBytes = dummyImageContent.getBytes();
        String url = "/items/1/image";

        when(itemService.getItemImage(1L)).thenReturn(Mono.just(imageBytes));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().bytes(imageBytes));
        verify(itemService, times(1)).getItemImage(1L);
    }
}