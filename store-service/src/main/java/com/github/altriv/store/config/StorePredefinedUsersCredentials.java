package com.github.altriv.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("store.predefined-users")
public record StorePredefinedUsersCredentials(List<String> credentials) {
}
