package com.github.altriv.paymentservice.service;

import com.github.altriv.paymentservice.controller.AddCreditsRq;
import com.github.altriv.paymentservice.controller.AddCreditsRs;
import com.github.altriv.paymentservice.domain.BalanceResponse;
import com.github.altriv.paymentservice.domain.PurchaseRequest;
import com.github.altriv.paymentservice.domain.PurchaseResponse;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<AddCreditsRs> addCredits(AddCreditsRq addCreditsRs);

    Mono<BalanceResponse> getBalance();

    Mono<PurchaseResponse> purchase(PurchaseRequest purchaseRq);
}
