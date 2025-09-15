package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17:5");

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
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll().block();
    }

    @Nested
    class GetCartTest {

        @Test
        void shouldReturnCartIfNotPaidOrderNotExists() {
            orderRepository.findFirstByPaidIsFalse()
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
            OrderEntity orderEntity = new OrderEntity(null, false, List.of(item, item2));

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
            OrderEntity orderEntity = new OrderEntity(null, false, List.of(item, item2));

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
    class BuyItemsTest {

        @Test
        void shouldSaveNotPaidOrderAsPaid() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            OrderEntity orderEntity = new OrderEntity(null, false, List.of(item, item2));

            orderRepository.save(orderEntity).block();

            orderService.buyItemsInCart()
                    .doOnNext(paidOrder -> {
                        assertNotNull(paidOrder);
                        assertEquals(orderEntity.getId(), paidOrder.id());
                        assertEquals(2, paidOrder.items().size());
                        assertTrue(paidOrder.items().containsAll(orderEntity.getItems()));
                    })
                    .block();
        }

        @Test
        void shouldNotSaveAnythingIfNotPaidOrderNotExists() {
            orderService.buyItemsInCart()
                    .doOnNext(Assertions::assertNull)
                    .block();
        }
    }

    @Test
    void shouldFindAllPaidOrders() {
        Item item = new Item(1L, "item title", "item description", 120, 5);
        Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
        Item item3 = new Item(3L, "item3 title", "item3 description", 1220, 2);
        OrderEntity orderEntity1 = new OrderEntity(null, true, List.of(item));
        OrderEntity orderEntity2 = new OrderEntity(null, false, List.of(item2));
        OrderEntity orderEntity3 = new OrderEntity(null, true, List.of(item3));

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
    void shouldFindPaidOrderById() {
        Item item = new Item(1L, "item title", "item description", 120, 5);
        OrderEntity orderEntity = new OrderEntity(null, true, List.of(item));
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
    void shouldReturnEmptyIfPaidOrderNotFoundById() {
        Item item = new Item(1L, "item title", "item description", 120, 5);
        OrderEntity orderEntity1 = new OrderEntity(null, true, List.of(item));
        orderRepository.save(orderEntity1)
                .flatMap(saved -> orderService.findPaidOrderById(saved.getId() + 1L).doOnNext(Assertions::assertNull))
                .block();
    }

}