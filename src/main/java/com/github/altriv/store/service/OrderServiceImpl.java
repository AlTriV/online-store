package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Cart> getNotPaidOrderAsCart() {
        return orderRepository.findFirstByPaidIsFalse()
                .map(orderEntity -> new Cart(orderEntity.getItems()));
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
        return orderRepository.findAllByPaidIsTrue()
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()));
    }

    @Override
    public Mono<Order> findPaidOrderById(long orderId) {
        return orderRepository.findFirstByPaidIsTrueAndId(orderId)
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()));
    }

    @Override
    public Mono<Order> buyItemsInCart() {
        return orderRepository.findFirstByPaidIsFalse()
                .doOnNext(orderEntity -> orderEntity.setPaid(true))
                .flatMap(orderRepository::save)
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                .as(transactionalOperator::transactional);
    }
}
