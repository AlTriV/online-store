package com.github.altriv.store.service;

import com.github.altriv.store.config.StoreCacheProperties;
import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private final String orderCachePrefix = "order:";

    private final String cacheTtl = "PT20S";

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private ReactiveRedisOperations<String, Order> orderRedisOperations;

    @Mock
    private ReactiveValueOperations<String, Order> orderValueOperations;

    @Mock
    private StoreCacheProperties cacheProperties;

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
                () -> orderService.saveCartAsNotPaidOrder(null).block()
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
        when(orderRepository.save(eq(expectedOrderEntity))).thenReturn(Mono.just(expectedOrderEntity));
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.saveCartAsNotPaidOrder(cart).block();

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
        when(orderRepository.save(eq(expectedOrderEntity))).thenReturn(Mono.just(expectedOrderEntity));
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.saveCartAsNotPaidOrder(cart).block();

        verify(orderRepository, times(1)).findFirstByPaidIsFalse();
        verify(orderRepository, times(1)).save(eq(expectedOrderEntity));
    }

    @Nested
    class GetAllPaidOrdersTest {

        @BeforeEach
        void setUp() {
            when(cacheProperties.ttl()).thenReturn(cacheTtl);
            when(cacheProperties.orderCachePrefix()).thenReturn(orderCachePrefix);
            when(orderRedisOperations.opsForValue()).thenReturn(orderValueOperations);
        }

        @Test
        void shouldReturnPaidOrdersFromCache() {
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            Item item3 = new Item(3L, "title3", "description3", 3000, 3);
            OrderEntity orderEntity = new OrderEntity(1L, false, List.of(item3));
            OrderEntity orderEntity2 = new OrderEntity(2L, false, List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Order expectedOrder2 = new Order(orderEntity2.getId(), orderEntity2.getItems());
            List<String> ordersInCacheKeys = List.of(orderCachePrefix + 1, orderCachePrefix + 2);

            when(orderRedisOperations.keys(eq(orderCachePrefix + "*"))).thenReturn(Flux.fromIterable(ordersInCacheKeys));
            when(orderValueOperations.get(orderCachePrefix + 1)).thenReturn(Mono.just(expectedOrder));
            when(orderValueOperations.get(orderCachePrefix + 2)).thenReturn(Mono.just(expectedOrder2));
            when(orderRepository.findAllByPaidIsTrue()).thenReturn(Flux.fromIterable(List.of()));

            orderService.getAllPaidOrders()
                    .collectList()
                    .doOnNext(paidOrders -> {
                        assertNotNull(paidOrders);
                        assertEquals(2, paidOrders.size());
                        assertTrue(paidOrders.containsAll(List.of(expectedOrder, expectedOrder2)));
                    })
                    .block();

            verify(orderRedisOperations, times(1)).keys(eq(orderCachePrefix + "*"));
            verify(orderValueOperations, times(1)).get(eq(orderCachePrefix + 1));
            verify(orderValueOperations, times(1)).get(eq(orderCachePrefix + 2));
        }

        @Test
        void shouldReturnPaidOrdersFromDatabase() {
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            Item item3 = new Item(3L, "title3", "description3", 3000, 3);
            OrderEntity orderEntity = new OrderEntity(1L, false, List.of(item3));
            OrderEntity orderEntity2 = new OrderEntity(2L, false, List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Order expectedOrder2 = new Order(orderEntity2.getId(), orderEntity2.getItems());
            String order1Key = orderCachePrefix + 1;
            String order2Key = orderCachePrefix + 2;
            Mono<Boolean> saveToCacheFlag = spy(Mono.just(true));

            Duration ttl = Duration.parse(cacheTtl);

            when(orderRedisOperations.keys(eq(orderCachePrefix + "*"))).thenReturn(Flux.empty());
            when(orderValueOperations.set(eq(order1Key), eq(expectedOrder), eq(ttl))).thenReturn(saveToCacheFlag);
            when(orderValueOperations.set(eq(order2Key), eq(expectedOrder2), eq(ttl))).thenReturn(saveToCacheFlag);
            when(saveToCacheFlag.thenReturn(expectedOrder)).thenReturn(Mono.just(expectedOrder));
            when(saveToCacheFlag.thenReturn(expectedOrder2)).thenReturn(Mono.just(expectedOrder2));
            when(orderRepository.findAllByPaidIsTrue()).thenReturn(Flux.fromIterable(List.of(orderEntity, orderEntity2)));

            orderService.getAllPaidOrders()
                    .collectList()
                    .doOnNext(paidOrders -> {
                        assertNotNull(paidOrders);
                        assertEquals(2, paidOrders.size());
                        assertTrue(paidOrders.containsAll(List.of(expectedOrder, expectedOrder2)));
                    })
                    .block();

            verify(orderRedisOperations, times(1)).keys(eq(orderCachePrefix + "*"));
            verify(orderValueOperations, times(1)).set(eq(order1Key), eq(expectedOrder), eq(ttl));
            verify(orderValueOperations, times(1)).set(eq(order2Key), eq(expectedOrder2), eq(ttl));
            verifyNoMoreInteractions(orderValueOperations);
        }
    }

    @Nested
    class FindPaidOrderByIdTest {

        @BeforeEach
        void setUp() {
            when(cacheProperties.ttl()).thenReturn(cacheTtl);
            when(cacheProperties.orderCachePrefix()).thenReturn(orderCachePrefix);
            when(orderRedisOperations.opsForValue()).thenReturn(orderValueOperations);
        }

        @Test
        void shouldReturnPaidOrderFromCache() {
            long orderId = 1L;
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            OrderEntity orderEntity = new OrderEntity(orderId, true, List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());

            when(orderValueOperations.get(orderCachePrefix + 1)).thenReturn(Mono.just(expectedOrder));
            when(orderRepository.findFirstByPaidIsTrueAndId(orderId)).thenReturn(Mono.empty());

            orderService.findPaidOrderById(orderId)
                    .doOnNext(paidOrder -> {
                        assertNotNull(paidOrder);
                        assertEquals(expectedOrder, paidOrder);
                    })
                    .block();
            verify(orderRedisOperations, times(1)).opsForValue();
            verify(orderValueOperations, times(1)).get(eq(orderCachePrefix + 1));
        }

        @Test
        void shouldReturnPaidOrderFromDatabase() {
            long orderId = 1L;
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            OrderEntity orderEntity = new OrderEntity(orderId, true, List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Duration ttl = Duration.parse(cacheTtl);

            Mono<Boolean> saveToCacheFlag = spy(Mono.just(true));

            when(orderValueOperations.get(orderCachePrefix + 1)).thenReturn(Mono.empty());
            when(orderRepository.findFirstByPaidIsTrueAndId(orderId)).thenReturn(Mono.just(orderEntity));
            when(orderValueOperations.set(eq(orderCachePrefix + 1), eq(expectedOrder), eq(ttl))).thenReturn(saveToCacheFlag);
            when(saveToCacheFlag.thenReturn(expectedOrder)).thenReturn(Mono.just(expectedOrder));

            orderService.findPaidOrderById(orderId)
                    .doOnNext(paidOrder -> {
                        assertNotNull(paidOrder);
                        assertEquals(expectedOrder, paidOrder);
                    })
                    .block();
            verify(orderRedisOperations, times(2)).opsForValue();
            verify(orderValueOperations, times(1)).get(eq(orderCachePrefix + 1));
            verify(orderRepository, times(1)).findFirstByPaidIsTrueAndId(orderId);
            verify(orderValueOperations, times(1)).set(eq(orderCachePrefix + 1), eq(expectedOrder), eq(ttl));
        }

        @Test
        void shouldReturnEmptyIfPaidOrderNotFound() {
            long orderId = 1L;

            when(orderValueOperations.get(orderCachePrefix + 1)).thenReturn(Mono.empty());
            when(orderRepository.findFirstByPaidIsTrueAndId(orderId)).thenReturn(Mono.empty());

            orderService.findPaidOrderById(orderId)
                    .doOnNext(Assertions::assertNull)
                    .block();

            verify(orderRepository, times(1)).findFirstByPaidIsTrueAndId(orderId);
        }
    }

    @Nested
    class SaveCartAsPaidOrderTest {

        @BeforeEach
        void setUp() {
            when(cacheProperties.ttl()).thenReturn(cacheTtl);
            when(cacheProperties.orderCachePrefix()).thenReturn(orderCachePrefix);
        }

        @Test
        void shouldReturnEmptyIfCartNotFound() {
            when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.empty());
            when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderService.saveCartAsPaidOrder()
                    .doOnNext(Assertions::assertNull)
                    .block();

            verify(orderRepository, times(1)).findFirstByPaidIsFalse();
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        void shouldReturnBoughtOrderAndPutInCache() {
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            OrderEntity orderEntity = new OrderEntity(1L, false, List.of(item, item2));
            OrderEntity paidOrderEntity = new OrderEntity(1L, true, List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Duration ttl = Duration.parse(cacheTtl);
            Mono<Boolean> saveToCacheFlag = spy(Mono.just(true));

            when(orderRedisOperations.opsForValue()).thenReturn(orderValueOperations);
            when(orderValueOperations.set(eq(orderCachePrefix + 1), eq(expectedOrder), eq(ttl))).thenReturn(saveToCacheFlag);
            when(saveToCacheFlag.thenReturn(expectedOrder)).thenReturn(Mono.just(expectedOrder));

            when(orderRepository.findFirstByPaidIsFalse()).thenReturn(Mono.just(orderEntity));
            when(orderRepository.save(eq(paidOrderEntity))).thenReturn(Mono.just(paidOrderEntity));
            when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderService.saveCartAsPaidOrder()
                    .doOnNext(order -> {
                        assertNotNull(order);
                        assertEquals(expectedOrder, order);
                    })
                    .block();

            verify(orderRepository, times(1)).findFirstByPaidIsFalse();
            verify(orderRepository, times(1)).save(paidOrderEntity);
            verify(orderRedisOperations, times(1)).opsForValue();
            verify(orderValueOperations, times(1)).set(eq(orderCachePrefix + 1), eq(expectedOrder), eq(ttl));
        }
    }
}