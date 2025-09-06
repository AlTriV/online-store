package com.github.altriv.store.controller;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.service.StoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

            when(storeService.getCart()).thenReturn(cart);

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

            when(storeService.getCart()).thenReturn(cart);

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

}