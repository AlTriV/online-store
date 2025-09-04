package com.github.altriv.store.repository;

import com.github.altriv.store.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {

    @Query("select item.image from ItemEntity item where item.id = :itemId")
    Optional<byte[]> getItemImage(long itemId);
}
