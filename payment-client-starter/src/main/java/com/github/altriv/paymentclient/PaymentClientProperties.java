package com.github.altriv.paymentclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.service")
public record PaymentClientProperties(String url) {
}
