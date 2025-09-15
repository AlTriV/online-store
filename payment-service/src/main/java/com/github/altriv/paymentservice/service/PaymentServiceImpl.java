package com.github.altriv.paymentservice.service;

import com.github.altriv.paymentservice.controller.AddCreditsRq;
import com.github.altriv.paymentservice.controller.AddCreditsRs;
import com.github.altriv.paymentservice.entity.Wallet;
import com.github.altriv.paymentservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;

    @Override
    public Mono<AddCreditsRs> addCredits(AddCreditsRq addCreditsRq) {
        return walletRepository.findById(addCreditsRq.walletId())
                .defaultIfEmpty(new Wallet(null, 0L))
                .map(wallet -> {
                    long newBalance = wallet.getBalance() + addCreditsRq.creditsAmount();
                    wallet.setBalance(newBalance);
                    return wallet;
                })
                .flatMap(walletRepository::save)
                .map(wallet -> new AddCreditsRs(true, wallet.getBalance()))
                .defaultIfEmpty(new AddCreditsRs(false, 0L));
    }
}
