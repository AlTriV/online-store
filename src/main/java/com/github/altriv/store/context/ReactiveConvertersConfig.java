package com.github.altriv.store.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.altriv.store.entity.converter.OrderItemsReadConverter;
import com.github.altriv.store.entity.converter.OrderItemsWriteConverter;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ReactiveConvertersConfig extends AbstractR2dbcConfiguration {

    private final ObjectMapper objectMapper;
    private final ReactiveDatasourceProperties properties;

    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(
                ConnectionFactoryOptions.builder()
                        .option(ConnectionFactoryOptions.DRIVER, properties.driver())
                        .option(ConnectionFactoryOptions.HOST, properties.host())
                        .option(ConnectionFactoryOptions.PORT, Integer.parseInt(properties.port()))
                        .option(ConnectionFactoryOptions.DATABASE, properties.database())
                        .option(ConnectionFactoryOptions.USER, properties.username())
                        .option(ConnectionFactoryOptions.PASSWORD, properties.password())
                        .option(Option.valueOf("schema"), properties.schema())
                        .build()
        );
    }

    @Override
    protected List<Object> getCustomConverters() {
        return List.of(
                new OrderItemsReadConverter(objectMapper),
                new OrderItemsWriteConverter(objectMapper)
        );
    }
}
