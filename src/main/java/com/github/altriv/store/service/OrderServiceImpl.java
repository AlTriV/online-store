package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @NonNull
    @Override
    public Cart getNotPaidOrderAsCart() {
        Optional<OrderEntity> notPaidOrder = orderRepository.findNotPaidOrder();
        return notPaidOrder
                .map(orderEntity -> new Cart(orderEntity.getItems()))
                .orElse(Cart.empty());
    }

    @Override
    @Transactional
    public void saveCartAsNotPaidOrder(@NonNull Cart cart) {
        Optional<OrderEntity> notPaidOrder = orderRepository.findNotPaidOrder();
        OrderEntity orderEntity = notPaidOrder.orElseGet(OrderEntity::new);
        orderEntity.setItems(cart.getItems());
        orderRepository.save(orderEntity);
    }

    @Override
    public List<Order> getAllPaidOrders() {
        return orderRepository.findPaidOrders().stream()
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()))
                .toList();
    }

    @Override
    public Optional<Order> findPaidOrderById(long orderId) {
        return orderRepository.findPaidOrderById(orderId)
                .map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()));
    }

    @Override
    @Transactional
    public Optional<Order> buyItemsInCart() {
        Optional<OrderEntity> notPaidOrder = orderRepository.findNotPaidOrder();
        notPaidOrder.ifPresent(orderEntity -> {
            orderEntity.setPaid(true);
            orderRepository.save(orderEntity);
        });
        return notPaidOrder.map(orderEntity -> new Order(orderEntity.getId(), orderEntity.getItems()));
    }
}
