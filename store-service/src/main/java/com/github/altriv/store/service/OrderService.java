package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Order;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Mono<Cart> getNotPaidOrderAsCart();

    Mono<Void> saveCartAsNotPaidOrder(@NonNull Cart cart);

    Flux<Order> getAllPaidOrders();

    Mono<Order> findPaidOrderById(long orderId);

    Mono<Order> buyItemsInCart();
}
