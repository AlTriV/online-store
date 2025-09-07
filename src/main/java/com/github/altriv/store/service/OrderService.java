package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Order;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    @NonNull
    Cart getNotPaidOrderAsCart();

    void saveCartAsNotPaidOrder(@NonNull Cart cart);

    List<Order> getAllPaidOrders();

    Optional<Order> findPaidOrderById(long orderId);

    Optional<Order> buyItemsInCart();
}
