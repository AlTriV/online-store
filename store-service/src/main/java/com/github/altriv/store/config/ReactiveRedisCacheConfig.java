package com.github.altriv.store.config;

import com.github.altriv.store.entity.ItemEntity;
import com.github.altriv.store.model.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ReactiveRedisCacheConfig {

    @Bean
    public ReactiveRedisOperations<String, ItemEntity> itemRedisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ItemEntity> serializer = new Jackson2JsonRedisSerializer<>(ItemEntity.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, ItemEntity> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, ItemEntity> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisOperations<String, Order> orderRedisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Order> serializer = new Jackson2JsonRedisSerializer<>(Order.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Order> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Order> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
