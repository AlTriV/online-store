package com.github.altriv.paymentclient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PaymentClientAutoConfiguration {

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }
}
