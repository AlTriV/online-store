package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public Cart getNotPaidOrderAsCart() {
        Optional<OrderEntity> notPaidOrder = orderRepository.findNotPaidOrder();
        return notPaidOrder
                .map(orderEntity -> new Cart(orderEntity.getItems()))
                .orElse(Cart.empty());
    }
}
