package com.github.altriv.store.context;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("r2dbc.datasource")
public record ReactiveDatasourceProperties(
        String driver,
        String host,
        String port,
        String database,
        String schema,
        String username,
        String password
) {
}
