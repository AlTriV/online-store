package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Test
    void shouldReturnEmptyCartIfUnpaidOrderNotFound() {
        when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.empty());

        orderService.getNotPaidOrderAsCart()
                .doOnNext(cart -> {
                    assertNotNull(cart);
                    assertNotNull(cart.getItems());
                    assertTrue(cart.getItems().isEmpty());
                })
                .block();
    }

    @Test
    void shouldReturnCartWithItemsFromNotPaidOrder() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        OrderEntity orderEntity = mock(OrderEntity.class);
        when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.just(orderEntity));
        when(orderEntity.getItems()).thenReturn(List.of(item, item2));

        orderService.getNotPaidOrderAsCart()
                .doOnNext(cart -> {
                    assertNotNull(cart);
                    List<Item> items = cart.getItems();
                    assertNotNull(items);
                    assertEquals(2, items.size());
                    assertTrue(items.containsAll(List.of(item, item2)));
                })
                .block();
    }

    @Test
    void shouldThrowNPEWhenCartIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> orderService.saveCartAsNotPaidOrder(null).subscribe()
        );
    }

    @Test
    void shouldSaveExistsNotPaidOrder() {
        OrderEntity orderEntity = new OrderEntity(1L, false, List.of());
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        Cart cart = new Cart(List.of(item, item2));
        OrderEntity expectedOrderEntity = new OrderEntity(1L, false, cart.getItems());
        when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.just(orderEntity));
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.saveCartAsNotPaidOrder(cart).subscribe();

        verify(orderRepository, times(1)).findFirstByPaidIsFalse();
        verify(orderRepository, times(1)).save(eq(expectedOrderEntity));
    }

    @Test
    void shouldSaveNewNotPaidOrder() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        Cart cart = new Cart(List.of(item, item2));
        OrderEntity expectedOrderEntity = new OrderEntity(null, false, cart.getItems());
        when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.saveCartAsNotPaidOrder(cart).subscribe();

        verify(orderRepository, times(1)).findFirstByPaidIsFalse();
        verify(orderRepository, times(1)).save(eq(expectedOrderEntity));
    }

    @Test
    void shouldReturnPaidOrders() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        Item item3 = new Item(3L, "title3", "description3", 3000, 3);
        OrderEntity orderEntity = new OrderEntity(1L, false, List.of(item3));
        OrderEntity orderEntity2 = new OrderEntity(2L, false, List.of(item, item2));
        Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
        Order expectedOrder2 = new Order(orderEntity2.getId(), orderEntity2.getItems());

        when(orderRepository.findAllByPaidIsTrue()).thenReturn(Flux.fromIterable(List.of(orderEntity, orderEntity2)));

        orderService.getAllPaidOrders()
                .collectList()
                .doOnNext(paidOrders -> {
                    assertNotNull(paidOrders);
                    assertEquals(2, paidOrders.size());
                    assertTrue(paidOrders.containsAll(List.of(expectedOrder, expectedOrder2)));
                })
                .block();

        verify(orderRepository, times(1)).findAllByPaidIsTrue();
    }

    @Test
    void shouldReturnPaidOrderIfFound() {
        long orderId = 1L;
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        OrderEntity orderEntity = new OrderEntity(orderId, true, List.of(item, item2));
        Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());

        when(orderRepository.findFirstByPaidIsTrueAndId(orderId)).thenReturn(Mono.just(orderEntity));

        orderService.findPaidOrderById(orderId)
                .doOnNext(paidOrder -> {
                    assertNotNull(paidOrder);
                    assertEquals(expectedOrder, paidOrder);
                })
                .block();
        verify(orderRepository, times(1)).findFirstByPaidIsTrueAndId(orderId);
    }

    @Test
    void shouldReturnEmptyIfPaidOrderNotFound() {
        long orderId = 1L;
        when(orderRepository.findFirstByPaidIsTrueAndId(orderId)).thenReturn(Mono.empty());

        orderService.findPaidOrderById(orderId)
                .doOnNext(Assertions::assertNull)
                .block();

        verify(orderRepository, times(1)).findFirstByPaidIsTrueAndId(orderId);
    }

    @Test
    void shouldReturnEmptyIfCartNotFound() {
        when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.buyItemsInCart()
                .doOnNext(Assertions::assertNull)
                .block();

        verify(orderRepository, times(1)).findFirstByPaidIsFalse();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void shouldReturnBoughtOrder() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        OrderEntity orderEntity = new OrderEntity(1L, false, List.of(item, item2));
        OrderEntity paidOrderEntity = new OrderEntity(1L, true, List.of(item, item2));
        Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());

        when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.just(orderEntity));
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.buyItemsInCart()
                .doOnNext(order -> {
                    assertNotNull(order);
                    assertEquals(expectedOrder, order);
                })
                .subscribe();

        verify(orderRepository, times(1)).findFirstByPaidIsFalse();
        verify(orderRepository, times(1)).save(paidOrderEntity);
    }
}