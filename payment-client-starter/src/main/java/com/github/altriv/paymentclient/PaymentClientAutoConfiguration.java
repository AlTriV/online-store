package com.github.altriv.paymentclient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PaymentClientProperties.class)
@ConditionalOnProperty(name = "payment.service.url")
public class PaymentClientAutoConfiguration {

    @Bean
    public PaymentClient paymentClient(PaymentClientProperties properties) {
        return new PaymentClient(properties.url());
    }
}
