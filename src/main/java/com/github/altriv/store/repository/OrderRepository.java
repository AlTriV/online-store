package com.github.altriv.store.repository;

import com.github.altriv.store.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("select o from OrderEntity o where o.paid = false")
    Optional<OrderEntity> findNotPaidOrder();

    @Query("select o from OrderEntity o where o.paid = true")
    Collection<OrderEntity> findPaidOrders();

    @Query("select o from OrderEntity o where o.paid = true and o.id = :orderId")
    Optional<OrderEntity> findPaidOrderById(Long orderId);
}
