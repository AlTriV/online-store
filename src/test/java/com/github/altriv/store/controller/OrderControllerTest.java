package com.github.altriv.store.controller;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldReturnAllOrdersPage() throws Exception {
        Order order = new Order(1L, List.of());
        Order order2 = new Order(2L, List.of());
        List<Order> orders = List.of(order, order2);
        String url = "/orders";
        when(orderService.getAllPaidOrders()).thenReturn(Flux.fromIterable(orders));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", orders));

        verify(orderService, times(1)).getAllPaidOrders();
    }

    @ParameterizedTest
    @CsvSource({
            "'orderId', 'falSe'",
            "'test', 'true'",
            "1, 'tRue1'"
    })
    void shouldReturnClientError(String orderId, String newOrder) throws Exception {
        String url = "/orders/" + orderId;

        mockMvc.perform(get(url).param("newOrder", newOrder))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "true", "false"})
    void shouldRedirectToAllOrdersIfOrderNotFound(String newOrder) throws Exception {
        long orderId = 1L;
        String url = "/orders/" + orderId;

        when(orderService.findPaidOrderById(orderId)).thenReturn(Mono.empty());

        mockMvc.perform(get(url).param("newOrder", newOrder))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/orders"));
        verify(orderService, times(1)).findPaidOrderById(orderId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "true", "false"})
    void shouldRedirectToOrderPageIfOrderFound(String newOrder) throws Exception {
        Item item1 = mock(Item.class);
        Item item2 = mock(Item.class);
        long orderId = 1L;
        Order order = new Order(orderId, List.of(item1, item2));
        String url = "/orders/" + orderId;
        when(orderService.findPaidOrderById(orderId)).thenReturn(Mono.just(order));

        mockMvc.perform(get(url).param("newOrder", newOrder))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", order));
        verify(orderService, times(1)).findPaidOrderById(orderId);
    }
}