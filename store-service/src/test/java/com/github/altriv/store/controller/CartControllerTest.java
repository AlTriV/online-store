package com.github.altriv.store.controller;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.model.Purchase;
import com.github.altriv.store.service.StoreService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private StoreService storeService;

    @Nested
    class CartItemsTest {

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturnNotEmptyCartPageWithPurchaseButton() {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            cart.putBalance(4000L);

            String url = "/cart/items";

            when(storeService.getCart()).thenReturn(Mono.just(cart));

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                    .expectBody(String.class)
                    .consumeWith(result -> {
                        String body = result.getResponseBody();
                        assertNotNull(body);

                        assertFalse(body.contains("<h1>"));

                        assertTrue(body.contains("<title>Корзина товаров</title>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/1/image\">"));
                        assertTrue(body.contains("<b>title</b>"));
                        assertTrue(body.contains("<b>1000 руб.</b>"));
                        assertTrue(body.contains("<tr><td>description</td></tr>"));
                        assertTrue(body.contains("<span>2</span>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/2/image\">"));
                        assertTrue(body.contains("<b>title2</b>"));
                        assertTrue(body.contains("<b>2000 руб.</b>"));
                        assertTrue(body.contains("<tr><td>description2</td></tr>"));
                        assertTrue(body.contains("<span>1</span>"));

                        assertTrue(body.contains("<b>Итого: 4000 руб.</b>"));
                        assertTrue(body.contains("<button>Купить</button>"));
                    });

            verify(storeService, times(1)).getCart();
        }

        @ParameterizedTest
        @CsvSource({
                "3000, 'Недостаточно средств для оплаты'",
                ", 'Нет информации о балансе, возможность оплаты временно недоступна'"
        })
        @WithMockUser(roles = "USER")
        void shouldReturnNotEmptyCartPageWithoutPurchaseButton(Long balance, String expectedErrorMessage) {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            cart.putBalance(balance);

            String url = "/cart/items";

            when(storeService.getCart()).thenReturn(Mono.just(cart));

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                    .expectBody(String.class)
                    .consumeWith(result -> {
                        String body = result.getResponseBody();
                        assertNotNull(body);

                        assertTrue(body.contains("<h1 style=\"text-align:center\">" + expectedErrorMessage + "</h1>"));

                        assertTrue(body.contains("<title>Корзина товаров</title>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/1/image\">"));
                        assertTrue(body.contains("<b>title</b>"));
                        assertTrue(body.contains("<b>1000 руб.</b>"));
                        assertTrue(body.contains("<tr><td>description</td></tr>"));
                        assertTrue(body.contains("<span>2</span>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/2/image\">"));
                        assertTrue(body.contains("<b>title2</b>"));
                        assertTrue(body.contains("<b>2000 руб.</b>"));
                        assertTrue(body.contains("<tr><td>description2</td></tr>"));
                        assertTrue(body.contains("<span>1</span>"));

                        assertTrue(body.contains("<b>Итого: 4000 руб.</b>"));
                        assertFalse(body.contains("<button>Купить</button>"));
                    });

            verify(storeService, times(1)).getCart();
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturnEmptyCartPage() {
            Cart cart = Cart.empty();

            String url = "/cart/items";

            when(storeService.getCart()).thenReturn(Mono.just(cart));

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                    .expectBody(String.class)
                    .consumeWith(result -> {
                        String body = result.getResponseBody();
                        assertNotNull(body);

                        assertTrue(body.contains("<title>Корзина товаров</title>"));
                        assertTrue(body.contains("<b>Итого: 0 руб.</b>"));
                        assertFalse(body.contains("<button>Купить</button>"));
                    });

            verify(storeService, times(1)).getCart();
        }
    }

    @Nested
    class ChangeItemCountInCartTest {

        @ParameterizedTest
        @ValueSource(strings = {"PLUS", "MINUS", "DELETE"})
        @WithMockUser(roles = "USER")
        void shouldPerformActionAndReturnToCartPage(String action) {
            long id = 1L;

            when(storeService.changeItemCountInCart(id, ItemAction.valueOf(action))).thenReturn(Mono.empty());

            String url = "/cart/items/" + id;

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("action", action);

            webTestClient.mutateWith(csrf())
                    .post().uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().valueEquals(HttpHeaders.LOCATION, "/cart/items");

            verify(storeService, times(1)).changeItemCountInCart(id, ItemAction.valueOf(action));
        }

        @ParameterizedTest
        @CsvSource({
                "'plus', 1",
                "'minus', 1",
                "'Delete', 1",
                "'DELETE', id"
        })
        @WithMockUser(roles = "USER")
        void shouldReturnClientError(String action, String id) {
            String url = "/cart/items/" + id;

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("action", action);

            webTestClient.post().uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .exchange()
                    .expectStatus().is4xxClientError();

            verifyNoInteractions(storeService);
        }
    }

    @Nested
    class BuyItems {

        @Test
        @WithMockUser(roles = "USER")
        void shouldRedirectToCartPageWithErrorBanner() {
            String url = "/cart/buy";

            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            Purchase purchase = Purchase.builder()
                    .cart(cart)
                    .success(false)
                    .errorMessage("Сервис оплаты недоступен. Попробуйте оплатить позже")
                    .build();

            when(storeService.buyItemsInCart()).thenReturn(Mono.just(purchase));

            webTestClient.mutateWith(csrf())
                    .post().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                    .expectBody(String.class)
                    .consumeWith(result -> {
                        String body = result.getResponseBody();
                        assertNotNull(body);

                        assertTrue(body.contains("<h1 style=\"text-align:center\">Сервис оплаты недоступен. Попробуйте оплатить позже</h1>"));

                        assertTrue(body.contains("<title>Корзина товаров</title>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/1/image\">"));
                        assertTrue(body.contains("<b>title</b>"));
                        assertTrue(body.contains("<b>1000 руб.</b>"));
                        assertTrue(body.contains("<tr><td>description</td></tr>"));
                        assertTrue(body.contains("<span>2</span>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/2/image\">"));
                        assertTrue(body.contains("<b>title2</b>"));
                        assertTrue(body.contains("<b>2000 руб.</b>"));
                        assertTrue(body.contains("<tr><td>description2</td></tr>"));
                        assertTrue(body.contains("<span>1</span>"));

                        assertTrue(body.contains("<b>Итого: 4000 руб.</b>"));
                        assertFalse(body.contains("<button>Купить</button>"));
                    });

            verify(storeService, times(1)).buyItemsInCart();
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldRedirectToPaidOrderPage() {
            String url = "/cart/buy";

            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            long orderId = 1L;
            Order order = new Order(orderId, List.of(item, item2));
            Purchase purchase = Purchase.builder()
                    .cart(cart)
                    .success(true)
                    .paidOrder(order)
                    .build();
            String expectedRedirectUrl = "/orders/" + orderId + "?newOrder=true";

            when(storeService.buyItemsInCart()).thenReturn(Mono.just(purchase));

            webTestClient.mutateWith(csrf())
                    .post().uri(url)
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().valueEquals(HttpHeaders.LOCATION, expectedRedirectUrl);

            verify(storeService, times(1)).buyItemsInCart();
        }
    }

}