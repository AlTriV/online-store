package com.github.altriv.paymentservice.controller;

import com.github.altriv.paymentservice.api.BalanceApi;
import com.github.altriv.paymentservice.api.PayApi;
import com.github.altriv.paymentservice.domain.BalanceRs;
import com.github.altriv.paymentservice.domain.PurchaseRq;
import com.github.altriv.paymentservice.domain.PurchaseRs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController implements BalanceApi, PayApi {

    @Override
    public Mono<ResponseEntity<BalanceRs>> getBalance(ServerWebExchange exchange) {
        return BalanceApi.super.getBalance(exchange);
    }

    @Override
    public Mono<ResponseEntity<PurchaseRs>> purchase(Mono<PurchaseRq> purchaseRq, ServerWebExchange exchange) {
        return PayApi.super.purchase(purchaseRq, exchange);
    }
}
