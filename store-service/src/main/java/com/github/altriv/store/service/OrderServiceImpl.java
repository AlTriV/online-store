package com.github.altriv.store.service;

import com.github.altriv.store.config.StoreCacheProperties;
import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TransactionalOperator transactionalOperator;
    private final ReactiveRedisOperations<String, Order> orderRedisOperations;
    private final StoreCacheProperties cacheProperties;

    @Override
    public Mono<Cart> getNotPaidOrderAsCart() {
        return orderRepository.findFirstByPaidIsFalse()
                .map(orderEntity -> new Cart(orderEntity.getItems()))
                .defaultIfEmpty(Cart.empty());
    }

    @Override
    public Mono<Void> saveCartAsNotPaidOrder(@NonNull Cart cart) {
        return orderRepository.findFirstByPaidIsFalse()
                .switchIfEmpty(Mono.just(new OrderEntity()))
                .doOnNext(orderEntity -> orderEntity.setItems(cart.getItems()))
                .flatMap(orderRepository::save)
                .as(transactionalOperator::transactional)
                .then();
    }

    @Override
    public Flux<Order> getAllPaidOrders() {
        String cachePrefix = cacheProperties.allOrdersCachePrefix();
        Duration ttl = Duration.parse(cacheProperties.ttl());
        return orderRedisOperations.keys(cachePrefix + "*").flatMap(orderRedisOperations.opsForValue()::get)
                .switchIfEmpty(orderRepository.findAllByPaidIsTrue()
                        .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                        .flatMap(order -> orderRedisOperations.opsForValue().set(cachePrefix + order.id(), order, ttl).thenReturn(order))
                );
    }

    @Override
    public Mono<Order> findPaidOrderById(long orderId) {
        String cachePrefix = cacheProperties.orderCachePrefix();
        Duration ttl = Duration.parse(cacheProperties.ttl());
        return orderRedisOperations.opsForValue().get(cachePrefix + orderId)
                .switchIfEmpty(orderRepository.findFirstByPaidIsTrueAndId(orderId)
                        .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                        .flatMap(order -> orderRedisOperations.opsForValue().set(cachePrefix + order.id(), order, ttl).thenReturn(order))
                );
    }

    @Override
    public Mono<Order> saveCartAsPaidOrder() {
        String cachePrefix = cacheProperties.allOrdersCachePrefix();
        return orderRepository.findFirstByPaidIsFalse()
                .doOnNext(orderEntity -> orderEntity.setPaid(true))
                .flatMap(orderRepository::save)
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                .as(transactionalOperator::transactional)
                .flatMap(order -> orderRedisOperations.delete(orderRedisOperations.keys(cachePrefix + "*")).thenReturn(order));
    }
}
