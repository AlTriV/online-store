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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrderServiceImpl.class)
class OrderServiceImplTest {

    private final String ORDER_CACHE_PREFIX = "order-user:";

    private final String ALL_ORDERS_CACHE_PREFIX = "all-orders-user:";

    private final String cacheTtl = "PT20S";

    @Autowired
    private OrderServiceImpl orderService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private TransactionalOperator transactionalOperator;

    @MockitoBean
    private ReactiveRedisOperations<String, Order> orderRedisOperations;

    @MockitoBean
    private ReactiveValueOperations<String, Order> orderValueOperations;

    @MockitoBean
    private StoreCacheProperties cacheProperties;

    @Test
    @WithMockUser(username = "user")
    void shouldReturnEmptyCartIfUnpaidOrderNotFound() {
        when(orderRepository.findFirstByPaidIsFalseAndUsername("user")).thenReturn(Mono.empty());

        orderService.getNotPaidOrderAsCart()
                .doOnNext(cart -> {
                    assertNotNull(cart);
                    assertNotNull(cart.getItems());
                    assertTrue(cart.getItems().isEmpty());
                })
                .block();
    }

    @Test
    @WithMockUser(username = "user")
    void shouldReturnCartWithItemsFromNotPaidOrder() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        OrderEntity orderEntity = mock(OrderEntity.class);
        when(orderRepository.findFirstByPaidIsFalseAndUsername("user")).thenReturn(Mono.just(orderEntity));
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
    @WithMockUser(username = "user")
    void shouldSaveExistsNotPaidOrder() {
        OrderEntity orderEntity = new OrderEntity(1L, false, "user", List.of());
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        Cart cart = new Cart(List.of(item, item2));
        OrderEntity expectedOrderEntity = new OrderEntity(1L, false, "user", cart.getItems());
        when(orderRepository.findFirstByPaidIsFalseAndUsername("user")).thenReturn(Mono.just(orderEntity));
        when(orderRepository.save(eq(expectedOrderEntity))).thenReturn(Mono.just(expectedOrderEntity));
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.saveCartAsNotPaidOrder(cart).block();

        verify(orderRepository, times(1)).findFirstByPaidIsFalseAndUsername("user");
        verify(orderRepository, times(1)).save(eq(expectedOrderEntity));
    }

    @Test
    @WithMockUser(username = "user")
    void shouldSaveNewNotPaidOrder() {
        Item item = new Item(1L, "title", "description", 1000, 1);
        Item item2 = new Item(2L, "title2", "description2", 2000, 2);
        Cart cart = new Cart(List.of(item, item2));
        OrderEntity expectedOrderEntity = new OrderEntity(null, false, "user", cart.getItems());
        when(orderRepository.findFirstByPaidIsFalseAndUsername("user")).thenReturn(Mono.empty());
        when(orderRepository.save(eq(expectedOrderEntity))).thenReturn(Mono.just(expectedOrderEntity));
        when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.saveCartAsNotPaidOrder(cart).block();

        verify(orderRepository, times(1)).findFirstByPaidIsFalseAndUsername("user");
        verify(orderRepository, times(1)).save(eq(expectedOrderEntity));
    }

    @Nested
    @WithMockUser(username = "user")
    class GetAllPaidOrdersTest {

        @BeforeEach
        void setUp() {
            when(cacheProperties.ttl()).thenReturn(cacheTtl);
            when(orderRedisOperations.opsForValue()).thenReturn(orderValueOperations);
        }

        @Test
        void shouldReturnPaidOrdersFromCache() {
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            Item item3 = new Item(3L, "title3", "description3", 3000, 3);
            OrderEntity orderEntity = new OrderEntity(1L, false, "user", List.of(item3));
            OrderEntity orderEntity2 = new OrderEntity(2L, false, "user", List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Order expectedOrder2 = new Order(orderEntity2.getId(), orderEntity2.getItems());
            List<String> ordersInCacheKeys = List.of(ALL_ORDERS_CACHE_PREFIX + 1, ALL_ORDERS_CACHE_PREFIX + 2);

            when(orderRedisOperations.keys(eq(ALL_ORDERS_CACHE_PREFIX + "*"))).thenReturn(Flux.fromIterable(ordersInCacheKeys));
            when(orderValueOperations.get(ALL_ORDERS_CACHE_PREFIX + 1)).thenReturn(Mono.just(expectedOrder));
            when(orderValueOperations.get(ALL_ORDERS_CACHE_PREFIX + 2)).thenReturn(Mono.just(expectedOrder2));
            when(orderRepository.findAllByPaidIsTrueAndUsername("user")).thenReturn(Flux.fromIterable(List.of()));

            orderService.getAllPaidOrders()
                    .collectList()
                    .doOnNext(paidOrders -> {
                        assertNotNull(paidOrders);
                        assertEquals(2, paidOrders.size());
                        assertTrue(paidOrders.containsAll(List.of(expectedOrder, expectedOrder2)));
                    })
                    .block();

            verify(orderRedisOperations, times(1)).keys(eq(ALL_ORDERS_CACHE_PREFIX + "*"));
            verify(orderValueOperations, times(1)).get(eq(ALL_ORDERS_CACHE_PREFIX + 1));
            verify(orderValueOperations, times(1)).get(eq(ALL_ORDERS_CACHE_PREFIX + 2));
        }

        @Test
        void shouldReturnPaidOrdersFromDatabase() {
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            Item item3 = new Item(3L, "title3", "description3", 3000, 3);
            OrderEntity orderEntity = new OrderEntity(1L, false, "user", List.of(item3));
            OrderEntity orderEntity2 = new OrderEntity(2L, false, "user", List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Order expectedOrder2 = new Order(orderEntity2.getId(), orderEntity2.getItems());
            String order1Key = ALL_ORDERS_CACHE_PREFIX + 1;
            String order2Key = ALL_ORDERS_CACHE_PREFIX + 2;
            Mono<Boolean> saveToCacheFlag = spy(Mono.just(true));

            Duration ttl = Duration.parse(cacheTtl);

            when(orderRedisOperations.keys(eq(ALL_ORDERS_CACHE_PREFIX + "*"))).thenReturn(Flux.empty());
            when(orderValueOperations.set(eq(order1Key), eq(expectedOrder), eq(ttl))).thenReturn(saveToCacheFlag);
            when(orderValueOperations.set(eq(order2Key), eq(expectedOrder2), eq(ttl))).thenReturn(saveToCacheFlag);
            when(saveToCacheFlag.thenReturn(expectedOrder)).thenReturn(Mono.just(expectedOrder));
            when(saveToCacheFlag.thenReturn(expectedOrder2)).thenReturn(Mono.just(expectedOrder2));
            when(orderRepository.findAllByPaidIsTrueAndUsername("user")).thenReturn(Flux.fromIterable(List.of(orderEntity, orderEntity2)));

            orderService.getAllPaidOrders()
                    .collectList()
                    .doOnNext(paidOrders -> {
                        assertNotNull(paidOrders);
                        assertEquals(2, paidOrders.size());
                        assertTrue(paidOrders.containsAll(List.of(expectedOrder, expectedOrder2)));
                    })
                    .block();

            verify(orderRedisOperations, times(1)).keys(eq(ALL_ORDERS_CACHE_PREFIX + "*"));
            verify(orderValueOperations, times(1)).set(eq(order1Key), eq(expectedOrder), eq(ttl));
            verify(orderValueOperations, times(1)).set(eq(order2Key), eq(expectedOrder2), eq(ttl));
            verifyNoMoreInteractions(orderValueOperations);
        }
    }

    @Nested
    @WithMockUser(username = "user")
    class FindPaidOrderByIdTest {

        @BeforeEach
        void setUp() {
            when(cacheProperties.ttl()).thenReturn(cacheTtl);
            when(orderRedisOperations.opsForValue()).thenReturn(orderValueOperations);
        }

        @Test
        void shouldReturnPaidOrderFromCache() {
            long orderId = 1L;
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            OrderEntity orderEntity = new OrderEntity(orderId, true, "user", List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());

            when(orderValueOperations.get(ORDER_CACHE_PREFIX + 1)).thenReturn(Mono.just(expectedOrder));
            when(orderRepository.findByPaidIsTrueAndIdAndUsername(orderId, "user")).thenReturn(Mono.empty());

            orderService.findPaidOrderById(orderId)
                    .doOnNext(paidOrder -> {
                        assertNotNull(paidOrder);
                        assertEquals(expectedOrder, paidOrder);
                    })
                    .block();
            verify(orderRedisOperations, times(1)).opsForValue();
            verify(orderValueOperations, times(1)).get(eq(ORDER_CACHE_PREFIX + 1));
        }

        @Test
        void shouldReturnPaidOrderFromDatabase() {
            long orderId = 1L;
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            OrderEntity orderEntity = new OrderEntity(orderId, true, "user", List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Duration ttl = Duration.parse(cacheTtl);

            Mono<Boolean> saveToCacheFlag = spy(Mono.just(true));

            when(orderValueOperations.get(ORDER_CACHE_PREFIX + 1)).thenReturn(Mono.empty());
            when(orderRepository.findByPaidIsTrueAndIdAndUsername(orderId, "user")).thenReturn(Mono.just(orderEntity));
            when(orderValueOperations.set(eq(ORDER_CACHE_PREFIX + 1), eq(expectedOrder), eq(ttl))).thenReturn(saveToCacheFlag);
            when(saveToCacheFlag.thenReturn(expectedOrder)).thenReturn(Mono.just(expectedOrder));

            orderService.findPaidOrderById(orderId)
                    .doOnNext(paidOrder -> {
                        assertNotNull(paidOrder);
                        assertEquals(expectedOrder, paidOrder);
                    })
                    .block();
            verify(orderRedisOperations, times(2)).opsForValue();
            verify(orderValueOperations, times(1)).get(eq(ORDER_CACHE_PREFIX + 1));
            verify(orderRepository, times(1)).findByPaidIsTrueAndIdAndUsername(orderId, "user");
            verify(orderValueOperations, times(1)).set(eq(ORDER_CACHE_PREFIX + 1), eq(expectedOrder), eq(ttl));
        }

        @Test
        void shouldReturnEmptyIfPaidOrderNotFound() {
            long orderId = 1L;

            when(orderValueOperations.get(ORDER_CACHE_PREFIX + 1)).thenReturn(Mono.empty());
            when(orderRepository.findByPaidIsTrueAndIdAndUsername(orderId, "user")).thenReturn(Mono.empty());

            orderService.findPaidOrderById(orderId)
                    .doOnNext(Assertions::assertNull)
                    .block();

            verify(orderRepository, times(1)).findByPaidIsTrueAndIdAndUsername(orderId, "user");
        }
    }

    @Nested
    class SaveCartAsPaidOrderTest {

        @Test
        @WithMockUser(username = "user")
        void shouldReturnEmptyIfCartNotFound() {
            when(orderRepository.findFirstByPaidIsFalseAndUsername("user")).thenReturn(Mono.empty());
            when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderService.saveCartAsPaidOrder()
                    .doOnNext(Assertions::assertNull)
                    .block();

            verify(orderRepository, times(1)).findFirstByPaidIsFalseAndUsername("user");
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        @WithMockUser(username = "user")
        void shouldReturnBoughtOrderAndClearAllOrdersCache() {
            Item item = new Item(1L, "title", "description", 1000, 1);
            Item item2 = new Item(2L, "title2", "description2", 2000, 2);
            OrderEntity orderEntity = new OrderEntity(1L, false, "user", List.of(item, item2));
            OrderEntity paidOrderEntity = new OrderEntity(1L, true, "user", List.of(item, item2));
            Order expectedOrder = new Order(orderEntity.getId(), orderEntity.getItems());
            Mono<Long> deleteCacheCount = spy(Mono.just(2L));
            List<String> keysInCache = List.of(ALL_ORDERS_CACHE_PREFIX + 3, ALL_ORDERS_CACHE_PREFIX + 4);
            Flux<String> keysFlux = Flux.fromIterable(keysInCache);

            when(orderRedisOperations.keys(ALL_ORDERS_CACHE_PREFIX + "*")).thenReturn(keysFlux);
            when(orderRedisOperations.delete(keysFlux)).thenReturn(deleteCacheCount);
            when(deleteCacheCount.thenReturn(expectedOrder)).thenReturn(Mono.just(expectedOrder));

            when(orderRepository.findFirstByPaidIsFalseAndUsername("user")).thenReturn(Mono.just(orderEntity));
            when(orderRepository.save(eq(paidOrderEntity))).thenReturn(Mono.just(paidOrderEntity));
            when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderService.saveCartAsPaidOrder()
                    .doOnNext(order -> {
                        assertNotNull(order);
                        assertEquals(expectedOrder, order);
                    })
                    .block();

            verify(orderRepository, times(1)).findFirstByPaidIsFalseAndUsername("user");
            verify(orderRepository, times(1)).save(paidOrderEntity);
            verify(orderRedisOperations, times(1)).keys(eq(ALL_ORDERS_CACHE_PREFIX + "*"));
            verify(orderRedisOperations, times(1)).delete(keysFlux);
        }
    }
}