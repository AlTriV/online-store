package com.github.altriv.paymentservice.service;

import com.github.altriv.paymentservice.controller.AddCreditsRq;
import com.github.altriv.paymentservice.entity.Wallet;
import com.github.altriv.paymentservice.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.sql.init.mode=always"}
)
@Testcontainers
class PaymentServiceImplIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17:5");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.r2dbc.url",
                () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName()
        );
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletRepository walletRepository;

    @BeforeEach
    void prepareDatabase() {
        walletRepository.deleteAll().subscribe();
    }

    @Nested
    class GetBalanceTest {

        @Test
        void shouldReturnEmptyIfWalletNotFound() {
            paymentService.getBalance()
                    .doOnNext(Assertions::assertNull)
                    .block();
        }

        @Test
        void shouldReturnBalanceIfWalletFound() {
            long balance = 100L;
            walletRepository.save(new Wallet(null, balance)).subscribe();

            paymentService.getBalance()
                    .doOnNext(balanceRs -> {
                        assertNotNull(balanceRs);
                        assertEquals(balance, balanceRs.getBalance());
                    })
                    .block();
        }
    }

    @Nested
    class AddCreditsTest {

        @Test
        void shouldCreateWalletAndAddCreditsIfWalletNotFound() {
            long balance = 100L;
            long walletId = 1L;
            AddCreditsRq addCreditsRq = new AddCreditsRq(walletId, balance);

            paymentService.addCredits(addCreditsRq)
                    .doOnNext(addCreditsRs -> {
                        assertNotNull(addCreditsRs);
                        assertTrue(addCreditsRs.success());
                        assertEquals(balance, addCreditsRs.balance());
                    })
                    .flatMap(addCreditsRs -> walletRepository.findById(walletId))
                    .doOnNext(Assertions::assertNotNull)
                    .block();
        }

        @Test
        void shouldAddCreditsIfWalletFound() {
            long balance = 1200L;
            long credits = 100L;

            Wallet savedWallet = walletRepository.save(new Wallet(null, balance)).block();
            Long walletId = savedWallet.getId();

            AddCreditsRq addCreditsRq = new AddCreditsRq(walletId, credits);

            paymentService.addCredits(addCreditsRq)
                    .doOnNext(addCreditsRs -> {
                        assertNotNull(addCreditsRs);
                        assertTrue(addCreditsRs.success());
                        assertEquals(balance + credits, addCreditsRs.balance());
                    })
                    .block();
        }
    }

}