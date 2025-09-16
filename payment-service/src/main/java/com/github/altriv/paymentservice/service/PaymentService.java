package com.github.altriv.paymentservice.service;

import com.github.altriv.paymentservice.controller.AddCreditsRq;
import com.github.altriv.paymentservice.controller.AddCreditsRs;
import com.github.altriv.paymentservice.domain.BalanceRs;
import com.github.altriv.paymentservice.domain.PurchaseRq;
import com.github.altriv.paymentservice.domain.PurchaseRs;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<AddCreditsRs> addCredits(AddCreditsRq addCreditsRs);

    Mono<BalanceRs> getBalance();

    Mono<PurchaseRs> purchase(PurchaseRq purchaseRq);
}
