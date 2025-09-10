package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @NonNull
    @Override
    public Cart getNotPaidOrderAsCart() {
        return orderRepository.findFirstByPaidIsFalse()
                .map(orderEntity -> new Cart(orderEntity.getItems()))
                .blockOptional()
                .orElse(Cart.empty());
    }

    @Override
    public void saveCartAsNotPaidOrder(@NonNull Cart cart) {
        orderRepository.findFirstByPaidIsFalse()
                .switchIfEmpty(Mono.just(new OrderEntity()))
                .map(orderEntity -> {
                    orderEntity.setItems(cart.getItems());
                    return orderEntity;
                })
                .flatMap(orderRepository::save)
                .subscribe();
    }

    @Override
    public List<Order> getAllPaidOrders() {
        return orderRepository.findAllByPaidIsTrue()
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                .collectList()
                .block();
    }

    @Override
    public Optional<Order> findPaidOrderById(long orderId) {
        return orderRepository.findFirstByPaidIsTrueAndId(orderId)
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                .blockOptional();
    }

    @Override
    public Optional<Order> buyItemsInCart() {
        return orderRepository.findFirstByPaidIsFalse()
                .doOnNext(orderEntity -> orderEntity.setPaid(true))
                .flatMap(orderRepository::save)
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                .blockOptional();
    }
}
