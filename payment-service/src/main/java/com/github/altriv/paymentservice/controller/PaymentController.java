package com.github.altriv.paymentservice.controller;

import com.github.altriv.paymentservice.api.BalanceApi;
import com.github.altriv.paymentservice.api.PayApi;
import com.github.altriv.paymentservice.domain.BalanceResponse;
import com.github.altriv.paymentservice.domain.PurchaseRequest;
import com.github.altriv.paymentservice.domain.PurchaseResponse;
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
    public Mono<ResponseEntity<BalanceResponse>> getBalance(String username, ServerWebExchange exchange) {
        log.info("Request to get balance");
        return paymentService.getBalance(username)
                .defaultIfEmpty(new BalanceResponse().balance(0L))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PurchaseResponse>> purchase(Mono<PurchaseRequest> purchaseRq, ServerWebExchange exchange) {
        log.info("Request to purchase order");
        return purchaseRq.flatMap(paymentService::purchase)
                .map(ResponseEntity::ok);
    }

    @PostMapping(path = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AddCreditsRs>> addCredits(@RequestBody AddCreditsRq addCreditsRq) {
        log.info("ADMIN. Request to add credits on wallet balance. Params: {}", addCreditsRq);
        return paymentService.addCredits(addCreditsRq)
                .map(response -> ResponseEntity.ok().body(response));
    }
}
