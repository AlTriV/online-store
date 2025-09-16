package com.github.altriv.paymentservice.controller;

import com.github.altriv.paymentservice.domain.BalanceRs;
import com.github.altriv.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

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
}