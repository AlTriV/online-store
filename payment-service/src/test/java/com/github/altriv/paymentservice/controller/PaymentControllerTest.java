package com.github.altriv.paymentservice.controller;

import com.github.altriv.paymentservice.domain.BalanceResponse;
import com.github.altriv.paymentservice.domain.PurchaseRequest;
import com.github.altriv.paymentservice.domain.PurchaseResponse;
import com.github.altriv.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    class GetBalanceTest {

        @Test
        void shouldReturnBalance() {
            long balanceAmount = 1234L;
            String username = "user";
            String urlTemplate = "/balance?username=%s";
            String url = String.format(urlTemplate, username);
            BalanceResponse expectedBalance = new BalanceResponse().balance(balanceAmount);


            when(paymentService.getBalance(username)).thenReturn(Mono.just(expectedBalance));

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(BalanceResponse.class).isEqualTo(expectedBalance);
            verify(paymentService, times(1)).getBalance(username);
        }

        @Test
        void shouldReturnEmptyBalanceIfNotFound() {
            String username = "user";
            String urlTemplate = "/balance?username=%s";
            String url = String.format(urlTemplate, username);
            BalanceResponse expectedBalance = new BalanceResponse().balance(0L);

            when(paymentService.getBalance(username)).thenReturn(Mono.empty());

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(BalanceResponse.class).isEqualTo(expectedBalance);
            verify(paymentService, times(1)).getBalance(username);
        }
    }

    @Nested
    class PurchaseTest {

        @ParameterizedTest
        @CsvSource({
                "true, ",
                "false, 'Have no enough credits'"
        })
        void shouldCompletePurchase(boolean purchaseResult, String errorMessage) {
            UUID requestId = UUID.randomUUID();
            PurchaseRequest purchaseRq = new PurchaseRequest(requestId, 100L, "user");
            PurchaseResponse expectedPurchase = new PurchaseResponse(requestId, "user", purchaseResult).errorMessage(errorMessage);
            when(paymentService.purchase(purchaseRq)).thenReturn(Mono.just(expectedPurchase));

            String url = "/pay";

            webTestClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(purchaseRq)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PurchaseResponse.class).isEqualTo(expectedPurchase);
            verify(paymentService, times(1)).purchase(purchaseRq);
        }

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {
                "{]",
                "{\"requestId\": \"77257e9c-ef11-461d-b278-cba0ease57d1\"}",
                "{\"price\": 500}",
                "{\"other\": \"json value\"}"
        })
        void shouldReturnBadRequestIfValidationFails(String content) {
            String url = "/pay";

            webTestClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(content)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    class AddCreditsTest {

        @Test
        void shouldAddCredits() {
            long startBalance = 200L;
            AddCreditsRq addCreditsRq = new AddCreditsRq(100L, "user");
            AddCreditsRs addCreditsRs = new AddCreditsRs(true, startBalance + 100);
            when(paymentService.addCredits(addCreditsRq)).thenReturn(Mono.just(addCreditsRs));

            String url = "/add";

            webTestClient.post().uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(addCreditsRq)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(AddCreditsRs.class).isEqualTo(addCreditsRs);
            verify(paymentService, times(1)).addCredits(addCreditsRq);
        }
    }
}