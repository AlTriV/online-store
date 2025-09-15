package com.github.altriv.store.repository;

import com.github.altriv.store.entity.ItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ItemRepository extends R2dbcRepository<ItemEntity, Long> {

    Flux<ItemEntity> findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);

    Mono<Long> countAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
}
