package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Test
    void shouldReturnEmptyCartIfUnpaidOrderNotFound() {
        when(orderRepository.findNotPaidOrder()).thenReturn(Optional.empty());

        Cart cart = orderServiceImpl.getNotPaidOrderAsCart();

        assertNotNull(cart);
        assertNotNull(cart.getItems());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void shouldReturnCartWithItemsFromNotPaidOrder() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        OrderEntity orderEntity = mock(OrderEntity.class);
        when(orderRepository.findNotPaidOrder()).thenReturn(Optional.of(orderEntity));
        when(orderEntity.getItems()).thenReturn(List.of(item, item2));

        Cart cart = orderServiceImpl.getNotPaidOrderAsCart();

        assertNotNull(cart);
        List<Item> items = cart.getItems();
        assertNotNull(items);
        assertEquals(2, items.size());
        assertTrue(items.containsAll(List.of(item, item2)));
    }
}