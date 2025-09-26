package com.github.altriv.store.controller;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnAllOrdersPage() {
        Item item1 = new Item(1L, "title1", "item1 description", 100, 5);
        Item item2 = new Item(2L, "title2", "item2 description", 200, 3);
        Order order = new Order(1L, List.of(item1, item2));
        Order order2 = new Order(2L, List.of(item2));
        List<Order> orders = List.of(order, order2);
        String url = "/orders";
        when(orderService.getAllPaidOrders()).thenReturn(Flux.fromIterable(orders));

        webTestClient.get().uri(url)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);

                    assertTrue(body.contains("<title>Заказы</title>"));

                    assertTrue(body.contains("<a href=\"/store/orders/1\">Заказ №1</a>"));
                    assertTrue(body.contains("<td>title1 (5 шт.) 500 руб.</td>"));
                    assertTrue(body.contains("<td>title2 (3 шт.) 600 руб.</td>"));
                    assertTrue(body.contains("<b>Сумма: 1100 руб.</b>"));

                    assertTrue(body.contains("<a href=\"/store/orders/2\">Заказ №2</a>"));
                    assertTrue(body.contains("<td>title2 (3 шт.) 600 руб.</td>"));
                    assertTrue(body.contains("<b>Сумма: 600 руб.</b>"));
                });

        verify(orderService, times(1)).getAllPaidOrders();
    }

    @ParameterizedTest
    @CsvSource({
            "'orderId', 'falSe'",
            "'test', 'true'",
            "1, 'tRue1'"
    })
    @WithMockUser(roles = "USER")
    void shouldReturnClientError(String orderId, String newOrder) {
        String url = String.format("/orders/%s?newOrder=%s", orderId, newOrder);

        webTestClient.get().uri(url)
                .exchange()
                .expectStatus().is4xxClientError();

        verifyNoInteractions(orderService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "true", "false"})
    @WithMockUser(roles = "USER")
    void shouldRedirectToAllOrdersIfOrderNotFound(String newOrder) {
        long orderId = 1L;
        String url = String.format("/orders/%d?newOrder=%s", orderId, newOrder);

        when(orderService.findPaidOrderById(orderId)).thenReturn(Mono.empty());

        webTestClient.get().uri(url)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals(HttpHeaders.LOCATION, "/orders");

        verify(orderService, times(1)).findPaidOrderById(orderId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "true", "false"})
    @WithMockUser(roles = "USER")
    void shouldRedirectToOrderPageIfOrderFound(String newOrder) {
        Item item1 = new Item(1L, "title1", "item1 description", 100, 5);
        Item item2 = new Item(2L, "title2", "item2 description", 200, 3);
        long orderId = 1L;
        Order order = new Order(orderId, List.of(item1, item2));
        String url = String.format("/orders/%s?newOrder=%s", orderId, newOrder);
        when(orderService.findPaidOrderById(orderId)).thenReturn(Mono.just(order));

        webTestClient.get().uri(url)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);

                    assertTrue(body.contains("<h2>Заказ №1</h2>"));

                    assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/1/image\">"));
                    assertTrue(body.contains("<b>title1</b>"));
                    assertTrue(body.contains("<b>5 шт.</b>"));
                    assertTrue(body.contains("<b>500 руб.</b>"));

                    assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/2/image\">"));
                    assertTrue(body.contains("<b>title2</b>"));
                    assertTrue(body.contains("<b>3 шт.</b>"));
                    assertTrue(body.contains("<b>600 руб.</b>"));

                    assertTrue(body.contains("<h3>Сумма: 1100 руб.</h3>"));
                    assertEquals(newOrder.equals("true"), body.contains("Поздравляем! Успешная покупка!"));

                });
        verify(orderService, times(1)).findPaidOrderById(orderId);
    }
}