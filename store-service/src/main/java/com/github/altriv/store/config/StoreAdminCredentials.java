package com.github.altriv.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("store.admin")
public record StoreAdminCredentials(String username, String password) {
}
