package com.github.altriv.store.controller;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.service.StoreService;
import org.junit.jupiter.api.Disabled;
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

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @Nested
    class CartItemsTest {

        @Test
        void shouldReturnNotEmptyCartPage() throws Exception {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));

            String url = "/cart/items";

            when(storeService.getCart()).thenReturn(Mono.just(cart));

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(view().name("cart"))
                    .andExpect(model().attributeExists("items"))
                    .andExpect(model().attribute("items", cart.getItems()))
                    .andExpect(model().attributeExists("total"))
                    .andExpect(model().attribute("total", cart.getTotalPrice()))
                    .andExpect(model().attributeExists("items"))
                    .andExpect(model().attribute("empty", cart.isEmpty()));
            verify(storeService, times(1)).getCart();
        }

        @Test
        void shouldReturnEmptyCartPage() throws Exception {
            Cart cart = Cart.empty();

            String url = "/cart/items";

            when(storeService.getCart()).thenReturn(Mono.just(cart));

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(view().name("cart"))
                    .andExpect(model().attributeExists("items"))
                    .andExpect(model().attribute("items", cart.getItems()))
                    .andExpect(model().attributeExists("total"))
                    .andExpect(model().attribute("total", cart.getTotalPrice()))
                    .andExpect(model().attributeExists("items"))
                    .andExpect(model().attribute("empty", cart.isEmpty()));
            verify(storeService, times(1)).getCart();
        }
    }

    @Nested
    class ChangeItemCountInCartTest {

        @ParameterizedTest
        @ValueSource(strings = {"PLUS", "MINUS", "DELETE"})
        void shouldPerformActionAndReturnToCartPage(String action) throws Exception {
            long id = 1L;

            when(storeService.changeItemCountInCart(id, ItemAction.valueOf(action))).thenReturn(Mono.empty());

            String url = "/cart/items/" + id;

            mockMvc.perform(multipart(url).param("action", action))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/cart/items"));
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
            String url = "/cart/items/" + id;

            mockMvc.perform(multipart(url).param("action", action))
                    .andExpect(status().is4xxClientError());
            verifyNoInteractions(storeService);
        }
    }
}