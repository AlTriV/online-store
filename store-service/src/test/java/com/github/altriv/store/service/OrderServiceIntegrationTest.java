package com.github.altriv.store.service;

import com.github.altriv.paymentclient.PaymentClient;
import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockitoBean(types = PaymentClient.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=com.github.altriv.paymentclient.PaymentClientAutoConfiguration",
        "store.cache.itemCachePrefix='item:'",
        "store.cache.ttl=PT3S",
        "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8082/realms/master",
        "spring.security.oauth2.client.registration.store-service.client-secret=123456"
})
@WithMockUser(username = "user")
class OrderServiceIntegrationTest {

    private static final String ORDER_CACHE_TEMPLATE = "order-%s:";
    private static final String ALL_ORDERS_CACHE_TEMPLATE = "all-orders-%s:";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17:5");

    @Container
    static RedisContainer redis = new RedisContainer("redis:8.2.1-bookworm");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.r2dbc.url",
                () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName()
        );
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReactiveRedisOperations<String, Order> orderRedisOperations;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll().block();
        orderRedisOperations.delete(orderRedisOperations.keys(format(ALL_ORDERS_CACHE_TEMPLATE, "user") + "*")).block();
        orderRedisOperations.delete(orderRedisOperations.keys(format(ORDER_CACHE_TEMPLATE, "user") + "*")).block();
    }

    @Nested

    class GetCartTest {

        @Test
        void shouldReturnCartIfNotPaidOrderNotExists() {
            orderRepository.findFirstByPaidIsFalseAndUsername("user")
                    .doOnNext(Assertions::assertNull)
                    .block();

            orderService.getNotPaidOrderAsCart()
                    .doOnNext(cart -> {
                        assertNotNull(cart);
                        assertTrue(cart.isEmpty());
                    })
                    .block();
        }

        @Test
        void shouldReturnCartIfPaidOrderExists() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            OrderEntity orderEntity = new OrderEntity(null, false, "user", List.of(item, item2));

            orderRepository.save(orderEntity).block();

            orderService.getNotPaidOrderAsCart()
                    .doOnNext(cart -> {
                        assertNotNull(cart);
                        assertFalse(cart.isEmpty());
                        assertEquals(2, cart.getItems().size());
                        assertTrue(cart.getItems().containsAll(List.of(item, item2)));
                    })
                    .block();
        }
    }

    @Nested
    @WithMockUser(username = "user")
    class SaveCartTest {

        @Test
        void shouldSaveNewCart() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            Cart cart = new Cart(List.of(item, item2));

            orderService.saveCartAsNotPaidOrder(cart).block();

            orderService.getNotPaidOrderAsCart()
                    .doOnNext(savedCart -> {
                        assertNotNull(savedCart);
                        assertFalse(savedCart.isEmpty());
                        assertEquals(2, savedCart.getItems().size());
                        assertTrue(savedCart.getItems().containsAll(cart.getItems()));
                    })
                    .block();
        }

        @Test
        void shouldReplaceItemsInExistingCart() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            OrderEntity orderEntity = new OrderEntity(null, false, "user", List.of(item, item2));

            orderRepository.save(orderEntity).block();

            Item item3 = new Item(3L, "item3 title", "item3 description", 1220, 2);
            Item item4 = new Item(4L, "item4 title", "item4 description", 520, 1);
            Cart cart = new Cart(List.of(item3, item4));

            orderService.saveCartAsNotPaidOrder(cart).block();

            orderService.getNotPaidOrderAsCart()
                    .doOnNext(savedCart -> {
                        assertNotNull(savedCart);
                        assertFalse(savedCart.isEmpty());
                        assertEquals(2, savedCart.getItems().size());
                        assertTrue(savedCart.getItems().containsAll(cart.getItems()));
                    })
                    .block();
        }
    }

    @Nested
    @WithMockUser(username = "user")
    class SavePaidOrderTest {

        @Test
        void shouldSaveNotPaidOrderAsPaidAndClearAllOrdersCache() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            OrderEntity cart = new OrderEntity(null, false, "user", List.of(item, item2));
            String allOrdersCachePrefix = format(ALL_ORDERS_CACHE_TEMPLATE, "user");

            orderRepository.save(cart).block();

            orderService.saveCartAsPaidOrder()
                    .doOnNext(paidOrder -> {
                        assertNotNull(paidOrder);
                        assertEquals(cart.getId(), paidOrder.id());
                        assertEquals(2, paidOrder.items().size());
                        assertTrue(paidOrder.items().containsAll(cart.getItems()));
                    })
                    .block();
            List<String> keysInCache = orderRedisOperations.keys(allOrdersCachePrefix + "*").collectList().block();
            assertTrue(keysInCache.isEmpty());
        }

        @Test
        void shouldNotSaveAnythingIfNotPaidOrderNotExists() {
            orderService.saveCartAsPaidOrder()
                    .doOnNext(Assertions::assertNull)
                    .block();
        }
    }

    @Nested
    @WithMockUser(username = "user")
    class FindAllPaidOrderTest {

        @Test
        void shouldFindAllFromDatabase() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            Item item3 = new Item(3L, "item3 title", "item3 description", 1220, 2);
            OrderEntity orderEntity1 = new OrderEntity(null, true, "user", List.of(item));
            OrderEntity orderEntity2 = new OrderEntity(null, false, "user", List.of(item2));
            OrderEntity orderEntity3 = new OrderEntity(null, true, "user", List.of(item3));

            orderRepository.saveAll(List.of(orderEntity1, orderEntity2, orderEntity3)).blockLast();

            Order expectedOrder1 = new Order(orderEntity1.getId(), orderEntity1.getItems());
            Order expectedOrder2 = new Order(orderEntity3.getId(), orderEntity3.getItems());

            orderService.getAllPaidOrders()
                    .collectList()
                    .doOnNext(paidOrders -> {
                        assertNotNull(paidOrders);
                        assertEquals(2, paidOrders.size());
                        assertTrue(paidOrders.containsAll(List.of(expectedOrder1, expectedOrder2)));
                    })
                    .block();
        }

        @Test
        @WithMockUser(username = "user")
        void shouldFindAllFromCache() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            Item item3 = new Item(3L, "item3 title", "item3 description", 1220, 2);
            OrderEntity orderEntity1 = new OrderEntity(null, true, "user", List.of(item));
            OrderEntity orderEntity2 = new OrderEntity(null, false, "user", List.of(item2));
            OrderEntity orderEntity3 = new OrderEntity(null, true, "user", List.of(item3));

            orderRepository.saveAll(List.of(orderEntity1, orderEntity2, orderEntity3)).blockLast();

            Order expectedOrder1 = new Order(orderEntity1.getId(), orderEntity1.getItems());

            String orderCacheKey = format(ALL_ORDERS_CACHE_TEMPLATE, "user") + orderEntity1.getId();
            orderRedisOperations.opsForValue().set(orderCacheKey, expectedOrder1, Duration.ofSeconds(1)).block();

            orderService.getAllPaidOrders()
                    .collectList()
                    .doOnNext(paidOrders -> {
                        assertNotNull(paidOrders);
                        assertEquals(1, paidOrders.size());
                        assertEquals(expectedOrder1, paidOrders.getFirst());
                    })
                    .block();
        }

    }

    @Nested
    @WithMockUser(username = "user")
    class FindPaidOrderByIdTest {

        @Test
        void shouldFindInDatabase() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            OrderEntity orderEntity = new OrderEntity(null, true, "user", List.of(item));
            orderRepository.save(orderEntity)
                    .flatMap(saved -> orderService.findPaidOrderById(saved.getId())
                            .doOnNext(paidOrderById -> {
                                assertNotNull(paidOrderById);
                                assertEquals(saved.getId(), paidOrderById.id());
                                assertEquals(1, paidOrderById.items().size());
                                assertTrue(paidOrderById.items().contains(item));
                            }))
                    .block();
        }

        @Test
        void shouldFindInCache() {
            Item item = new Item(2L, "item title", "item description", 120, 5);

            long orderId = 1L;
            Order expectedOrder1 = new Order(orderId, List.of(item));

            String orderCacheKey = format(ORDER_CACHE_TEMPLATE, "user") + expectedOrder1.id();
            orderRedisOperations.opsForValue().set(orderCacheKey, expectedOrder1, Duration.ofSeconds(1)).block();

            orderService.findPaidOrderById(orderId)
                    .doOnNext(paidOrderById -> {
                        assertNotNull(paidOrderById);
                        assertEquals(orderId, paidOrderById.id());
                        assertEquals(1, paidOrderById.items().size());
                        assertTrue(paidOrderById.items().contains(item));
                    })
                    .block();
        }

        @Test
        void shouldReturnEmptyIfPaidOrderNotFoundById() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            OrderEntity orderEntity1 = new OrderEntity(null, true, "user", List.of(item));
            orderRepository.save(orderEntity1)
                    .flatMap(saved -> orderService.findPaidOrderById(saved.getId() + 1L).doOnNext(Assertions::assertNull))
                    .block();
        }
    }

}