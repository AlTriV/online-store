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
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private ItemService itemService;

    @Nested
    @WithMockUser(roles = "USER")
    class GetItem {

        @Test
        void shouldRedirectToMainPageIfItemNotFound() {
            long itemId = 1L;
            String url = "/items/" + itemId;

            when(storeService.getItemWithCartCount(itemId)).thenReturn(Mono.empty());

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().valueEquals(HttpHeaders.LOCATION, "/main/items");

            verify(storeService, times(1)).getItemWithCartCount(itemId);
        }

        @Test
        void shouldReturnItemPage() {
            long itemId = 1L;
            String url = "/items/" + itemId;
            Item item = new Item(itemId, "title1", "item1 description", 1200, 5);

            when(storeService.getItemWithCartCount(itemId)).thenReturn(Mono.just(item));

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/html")
                    .expectBody(String.class)
                    .consumeWith(result -> {
                        String body = result.getResponseBody();
                        assertNotNull(body);

                        assertTrue(body.contains("<title>Витрина товаров</title>"));
                        assertTrue(body.contains("<b>title1</b>"));
                        assertTrue(body.contains("<span>item1 description</span>"));
                        assertTrue(body.contains("<span>5</span>"));
                    });

            verify(storeService, times(1)).getItemWithCartCount(itemId);
        }
    }

    @Nested
    @WithMockUser(roles = "USER")
    class ChangeItemCountTest {

        @ParameterizedTest
        @ValueSource(strings = {"PLUS", "MINUS", "DELETE"})
        void shouldPerformActionAndReturnToMainPage(String action) {
            long id = 1L;

            when(storeService.changeItemCountInCart(id, ItemAction.valueOf(action))).thenReturn(Mono.empty());

            String url = "/items/" + id;

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("action", action);

            webTestClient.mutateWith(csrf())
                    .post().uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .expectHeader().valueEquals(HttpHeaders.LOCATION, url);

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
            String url = "/items/" + id;

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("action", action);

            webTestClient.mutateWith(csrf())
                    .post().uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .exchange()
                    .expectStatus().is4xxClientError();

            verifyNoInteractions(storeService);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "dummy_image_content"})
    @WithMockUser(roles = "USER")
    void shouldReturnBytesOfImage(String dummyImageContent) {
        byte[] imageBytes = dummyImageContent.getBytes();
        String url = "/items/1/image";

        when(itemService.getItemImage(1L)).thenReturn(Mono.just(imageBytes));

        webTestClient.get().uri(url)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).isEqualTo(imageBytes);

        verify(itemService, times(1)).getItemImage(1L);
    }
}