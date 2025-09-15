package com.github.altriv.paymentservice.controller;

import com.github.altriv.paymentservice.api.BalanceApi;
import com.github.altriv.paymentservice.api.PayApi;
import com.github.altriv.paymentservice.domain.BalanceRs;
import com.github.altriv.paymentservice.domain.PurchaseRq;
import com.github.altriv.paymentservice.domain.PurchaseRs;
import com.github.altriv.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController implements BalanceApi, PayApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BalanceRs>> getBalance(ServerWebExchange exchange) {
        return BalanceApi.super.getBalance(exchange);
    }

    @Override
    public Mono<ResponseEntity<PurchaseRs>> purchase(Mono<PurchaseRq> purchaseRq, ServerWebExchange exchange) {
        return PayApi.super.purchase(purchaseRq, exchange);
    }

    @PostMapping(path = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AddCreditsRs>> addCredits(@RequestBody AddCreditsRq addCreditsRq) {
        log.info("ADMIN. Request to add credits on wallet balance. Params: {}", addCreditsRq);
        return paymentService.addCredits(addCreditsRq)
                .map(response -> ResponseEntity.ok().body(response));
    }
}
