package com.github.altriv.store.repository;

import com.github.altriv.store.entity.OrderEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<OrderEntity, Long> {

    Mono<OrderEntity> findFirstByPaidIsFalseAndUsername(String username);

    Flux<OrderEntity> findAllByPaidIsTrueAndUsername(String username);

    Mono<OrderEntity> findByPaidIsTrueAndIdAndUsername(Long id, String username);
}
