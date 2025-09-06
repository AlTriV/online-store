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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldReturnEmptyCartIfUnpaidOrderNotFound() {
        when(orderRepository.findNotPaidOrder()).thenReturn(Optional.empty());

        Cart cart = orderService.getNotPaidOrderAsCart();

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

        Cart cart = orderService.getNotPaidOrderAsCart();

        assertNotNull(cart);
        List<Item> items = cart.getItems();
        assertNotNull(items);
        assertEquals(2, items.size());
        assertTrue(items.containsAll(List.of(item, item2)));
    }

    @Test
    void shouldThrowNPEWhenCartIsNull() {
        assertThrows(NullPointerException.class, () -> orderService.saveCartAsNotPaidOrder(null));
    }

    @Test
    void shouldSaveExistsNotPaidOrder() {
        OrderEntity orderEntity = new OrderEntity(1L, false, List.of());
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        Cart cart = new Cart(List.of(item, item2));
        OrderEntity expectedOrderEntity = new OrderEntity(1L, false, cart.getItems());
        when(orderRepository.findNotPaidOrder()).thenReturn(Optional.of(orderEntity));

        orderService.saveCartAsNotPaidOrder(cart);

        verify(orderRepository, times(1)).findNotPaidOrder();
        verify(orderRepository, times(1)).save(eq(expectedOrderEntity));
    }

    @Test
    void shouldSaveNewNotPaidOrder() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        Cart cart = new Cart(List.of(item, item2));
        OrderEntity expectedOrderEntity = new OrderEntity(null, false, cart.getItems());

        orderService.saveCartAsNotPaidOrder(cart);

        verify(orderRepository, times(1)).findNotPaidOrder();
        verify(orderRepository, times(1)).save(eq(expectedOrderEntity));
    }
}