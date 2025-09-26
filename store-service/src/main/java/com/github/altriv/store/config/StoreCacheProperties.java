package com.github.altriv.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("store.cache")
public record StoreCacheProperties(String itemCachePrefix,
                                   String ttl) {
}
