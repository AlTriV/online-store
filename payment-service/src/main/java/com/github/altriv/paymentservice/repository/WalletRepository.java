package com.github.altriv.paymentservice.repository;

import com.github.altriv.paymentservice.entity.Wallet;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends R2dbcRepository<Wallet, Long> {
}
