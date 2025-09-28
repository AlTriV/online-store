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

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;

    @Override
    public Mono<AddCreditsRs> addCredits(AddCreditsRq addCreditsRq) {
        return walletRepository.findByUsername(addCreditsRq.username())
                .defaultIfEmpty(new Wallet(null, 0L, addCreditsRq.username()))
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
    public Mono<BalanceResponse> getBalance(String username) {
        return findWalletForUser(username)
                .map(wallet -> new BalanceResponse().balance(wallet.getBalance()).username(wallet.getUsername()));
    }

    @Override
    public Mono<PurchaseResponse> purchase(PurchaseRequest purchaseRequest) {
        return findWalletForUser(purchaseRequest.getUsername())
                .filter(Objects::nonNull)
                .filter(wallet -> wallet.getBalance() >= purchaseRequest.getPrice())
                .flatMap(wallet -> {
                    wallet.setBalance(wallet.getBalance() - purchaseRequest.getPrice());
                    return walletRepository.save(wallet);
                })
                .map(wallet -> new PurchaseResponse().purchaseResult(true).requestId(purchaseRequest.getRequestId()).username(wallet.getUsername()))
                .defaultIfEmpty(new PurchaseResponse()
                        .purchaseResult(false)
                        .requestId(purchaseRequest.getRequestId())
                        .username(purchaseRequest.getUsername())
                        .errorMessage("Недостаточно средств для оплаты")
                );
    }

    private Mono<Wallet> findWalletForUser(String username) {
        return walletRepository.findByUsername(username)
                .defaultIfEmpty(new Wallet(null, 0L, username));
    }
}
