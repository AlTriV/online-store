package com.github.altriv.store.controller;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(MainController.class)
class MainControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private StoreService storeService;

    @Test
    void startTest() {
        String url = "/";

        webTestClient.get().uri(url)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals(HttpHeaders.LOCATION, "/main/items");
    }

    @Nested
    class GetItemsTest {

        @Test
        void shouldReturnItemPageIfDefaultParams() {
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

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                    .expectBody(String.class)
                    .consumeWith(result -> {
                        String body = result.getResponseBody();
                        assertNotNull(body);

                        assertTrue(body.contains("<title>Витрина товаров</title>"));
                        assertTrue(body.contains("<option value=\"10\" selected=\"selected\">10</option>"));
                        assertTrue(body.contains("<span>Страница: 1</span>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/1/image\">"));
                        assertTrue(body.contains("<b>title</b>"));
                        assertTrue(body.contains("<b>1000 руб.</b>"));
                        assertTrue(body.contains("<td>description</td>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/2/image\">"));
                        assertTrue(body.contains("<b>title2</b>"));
                        assertTrue(body.contains("<b>2000 руб.</b>"));
                        assertTrue(body.contains("<td>description2</td>"));
                    });

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
        void shouldReturnItemPageIfSearchParams(String search, String sort, int pageNumber, int pageSize) {
            ItemSorting itemSorting = ItemSorting.valueOf(sort);
            Item item = new Item(1L, "title", "description", 1000, 0);
            Item item2 = new Item(2L, "title2", "description2", 2000, 0);
            ItemsPage itemsPage = new ItemsPage(
                    List.of(item, item2),
                    new PageInfo(pageNumber, pageSize, false)
            );
            when(storeService.searchItems(search, itemSorting, pageNumber, pageSize)).thenReturn(Mono.just(itemsPage));

            String urlTemplate = "/main/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d";
            String url = String.format(urlTemplate, search, sort, pageNumber, pageSize);

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                    .expectBody(String.class)
                    .consumeWith(result -> {
                        String body = result.getResponseBody();
                        assertNotNull(body);

                        assertTrue(body.contains("<title>Витрина товаров</title>"));
                        assertTrue(body.contains(String.format("<option value=\"%d\" selected=\"selected\">%d</option>", pageSize, pageSize)));
                        assertTrue(body.contains(String.format("<span>Страница: %d</span>", pageNumber)));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/1/image\">"));
                        assertTrue(body.contains("<b>title</b>"));
                        assertTrue(body.contains("<b>1000 руб.</b>"));
                        assertTrue(body.contains("<td>description</td>"));

                        assertTrue(body.contains("<img width=\"300\" height=\"300\" src=\"http://localhost:8080/store/items/2/image\">"));
                        assertTrue(body.contains("<b>title2</b>"));
                        assertTrue(body.contains("<b>2000 руб.</b>"));
                        assertTrue(body.contains("<td>description2</td>"));
                    });

            verify(storeService, times(1)).searchItems(search, itemSorting, pageNumber, pageSize);
        }
    }

    @Nested
    class ChangeItemCountTest {

        @ParameterizedTest
        @ValueSource(strings = {"PLUS", "MINUS", "DELETE"})
        void shouldPerformActionAndReturnToMainPage(String action) {
            long id = 1L;

            String url = "/main/items/" + id;

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("action", action);

            when(storeService.changeItemCountInCart(id, ItemAction.valueOf(action))).thenReturn(Mono.empty());

            webTestClient.post().uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().valueEquals(HttpHeaders.LOCATION, "/main/items");

            verify(storeService, times(1)).changeItemCountInCart(id, ItemAction.valueOf(action));
        }

        @ParameterizedTest
        @CsvSource({
                "'plus', 1",
                "'minus', 1",
                "'Delete', 1",
                "'DELETE', id"
        })
        void shouldReturnClientError(String action, String id) {
            String url = "/main/items/" + id;

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

}