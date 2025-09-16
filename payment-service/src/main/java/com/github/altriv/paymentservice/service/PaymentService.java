package com.github.altriv.paymentservice.service;

import com.github.altriv.paymentservice.controller.AddCreditsRq;
import com.github.altriv.paymentservice.controller.AddCreditsRs;
import com.github.altriv.paymentservice.domain.BalanceRs;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<AddCreditsRs> addCredits(AddCreditsRq addCreditsRs);

    Mono<BalanceRs> getBalance();
}
