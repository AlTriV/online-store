package com.github.altriv.paymentservice.service;

import com.github.altriv.paymentservice.controller.AddCreditsRq;
import com.github.altriv.paymentservice.controller.AddCreditsRs;
import com.github.altriv.paymentservice.domain.BalanceResponse;
import com.github.altriv.paymentservice.domain.PurchaseRequest;
import com.github.altriv.paymentservice.domain.PurchaseResponse;
import com.github.altriv.paymentservice.entity.Wallet;
import com.github.altriv.paymentservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

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

    @Override
    public Mono<BalanceResponse> getBalance() {
        return findWallet()
                .map(wallet -> new BalanceResponse().balance(wallet.getBalance()));
    }

    @Override
    public Mono<PurchaseResponse> purchase(PurchaseRequest purchaseRq) {
        return findWallet()
                .filter(Objects::nonNull)
                .filter(wallet -> wallet.getBalance() >= purchaseRq.getPrice())
                .flatMap(wallet -> {
                    wallet.setBalance(wallet.getBalance() - purchaseRq.getPrice());
                    return walletRepository.save(wallet);
                })
                .map(wallet -> new PurchaseResponse().purchaseResult(true).requestId(purchaseRq.getRequestId()))
                .defaultIfEmpty(new PurchaseResponse()
                        .purchaseResult(false)
                        .requestId(purchaseRq.getRequestId())
                        .errorMessage("Недостаточно средств для оплаты")
                );
    }

    private Mono<Wallet> findWallet() {
        return walletRepository.findAll()
                .collectList()
                .filter(list -> !list.isEmpty())
                .map(List::getFirst);
    }
}
