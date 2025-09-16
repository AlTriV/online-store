package com.github.altriv.paymentservice.controller;

import com.github.altriv.paymentservice.domain.BalanceRs;
import com.github.altriv.paymentservice.domain.PurchaseRq;
import com.github.altriv.paymentservice.domain.PurchaseRs;
import com.github.altriv.paymentservice.domain.UnexpectedError;
import com.github.altriv.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
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
            String url = "/balance";
            BalanceRs expectedBalance = new BalanceRs().balance(balanceAmount);


            when(paymentService.getBalance()).thenReturn(Mono.just(expectedBalance));

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(BalanceRs.class).isEqualTo(expectedBalance);
            verify(paymentService, times(1)).getBalance();
        }

        @Test
        void shouldReturnEmptyBalanceIfNotFound() {
            String url = "/balance";
            BalanceRs expectedBalance = new BalanceRs().balance(0L);


            when(paymentService.getBalance()).thenReturn(Mono.empty());

            webTestClient.get().uri(url)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(BalanceRs.class).isEqualTo(expectedBalance);
            verify(paymentService, times(1)).getBalance();
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
            PurchaseRq purchaseRq = new PurchaseRq(requestId, 100L);
            PurchaseRs expectedPurchase = new PurchaseRs(requestId, purchaseResult).errorMessage(errorMessage);
            when(paymentService.purchase(purchaseRq)).thenReturn(Mono.just(expectedPurchase));

            String url = "/pay";

            webTestClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(purchaseRq)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PurchaseRs.class).isEqualTo(expectedPurchase);
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
            AddCreditsRq addCreditsRq = new AddCreditsRq(1L, 100);
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

    @Test
    void shouldReturnInternalServerErrorExceptionOccurred() {
        String url = "/balance";
        UnexpectedError error = new UnexpectedError(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "Unexpected error occurred");

        when(paymentService.getBalance()).thenThrow(new RuntimeException("Unexpected error"));

        webTestClient.get().uri(url)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(UnexpectedError.class).isEqualTo(error);
        verify(paymentService, times(1)).getBalance();
    }
}