package com.github.altriv.store.service;

import com.github.altriv.store.config.StoreCacheProperties;
import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_CACHE_TEMPLATE = "order-%s:";
    private static final String ALL_ORDERS_CACHE_TEMPLATE = "all-orders-%s:";

    private final OrderRepository orderRepository;
    private final TransactionalOperator transactionalOperator;
    private final ReactiveRedisOperations<String, Order> orderRedisOperations;
    private final StoreCacheProperties cacheProperties;

    @Override
    public Mono<Cart> getNotPaidOrderAsCart() {
        return getCurrentUsername()
                .flatMap(orderRepository::findFirstByPaidIsFalseAndUsername)
                .map(orderEntity -> new Cart(orderEntity.getItems()))
                .defaultIfEmpty(Cart.empty());
    }

    @Override
    public Mono<Void> saveCartAsNotPaidOrder(@NonNull Cart cart) {
        return getCurrentUsername()
                .flatMap(orderRepository::findFirstByPaidIsFalseAndUsername)
                .switchIfEmpty(getCurrentUsername().map(username -> new OrderEntity(null, false, username, null)))
                .doOnNext(orderEntity -> orderEntity.setItems(cart.getItems()))
                .flatMap(orderRepository::save)
                .as(transactionalOperator::transactional)
                .then();
    }

    @Override
    public Flux<Order> getAllPaidOrders() {
        Duration ttl = Duration.parse(cacheProperties.ttl());
        return getCurrentUsername()
                .flux()
                .flatMap(username -> orderRedisOperations.keys(format(ALL_ORDERS_CACHE_TEMPLATE, username) + "*")
                        .flatMap(orderRedisOperations.opsForValue()::get)
                        .switchIfEmpty(orderRepository.findAllByPaidIsTrueAndUsername(username)
                                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                                .flatMap(order -> orderRedisOperations.opsForValue()
                                        .set(format(ALL_ORDERS_CACHE_TEMPLATE, username) + order.id(), order, ttl)
                                        .thenReturn(order))
                        )
                );
    }

    @Override
    public Mono<Order> findPaidOrderById(long orderId) {
        Duration ttl = Duration.parse(cacheProperties.ttl());
        return getCurrentUsername()
                .flatMap(username -> orderRedisOperations.opsForValue().get(format(ORDER_CACHE_TEMPLATE, username) + orderId)
                        .switchIfEmpty(orderRepository.findByPaidIsTrueAndIdAndUsername(orderId, username)
                                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                                .flatMap(order ->
                                        orderRedisOperations.opsForValue()
                                                .set(format(ORDER_CACHE_TEMPLATE, username) + order.id(), order, ttl)
                                                .thenReturn(order)
                                )
                        )
                );
    }

    @Override
    public Mono<Order> saveCartAsPaidOrder() {
        return getCurrentUsername()
                .flatMap(username -> orderRepository.findFirstByPaidIsFalseAndUsername(username)
                        .doOnNext(orderEntity -> orderEntity.setPaid(true))
                        .flatMap(orderRepository::save)
                        .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                        .as(transactionalOperator::transactional)
                        .flatMap(order ->
                                orderRedisOperations
                                        .delete(orderRedisOperations.keys(format(ALL_ORDERS_CACHE_TEMPLATE, username) + "*"))
                                        .thenReturn(order))
                );
    }

    private Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName);
    }
}
