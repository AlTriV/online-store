package com.github.altriv.store.service;

import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.repository.OrderRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:17:5");
    }

    @DynamicPropertySource
    static void registerPostgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Nested
    class GetCartTest {

        @Test
        void shouldReturnCartIfNotPaidOrderNotExists() {
            Optional<OrderEntity> notPaidOrder = orderRepository.findNotPaidOrder();

            Cart cart = orderService.getNotPaidOrderAsCart();

            assertTrue(notPaidOrder.isEmpty());
            assertNotNull(cart);
            assertTrue(cart.isEmpty());
        }

        @Test
        void shouldReturnCartIfPaidOrderExists() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            OrderEntity orderEntity = new OrderEntity(null, false, List.of(item, item2));
            orderRepository.save(orderEntity);

            Cart cart = orderService.getNotPaidOrderAsCart();

            assertNotNull(cart);
            assertFalse(cart.isEmpty());
            assertEquals(2, cart.getItems().size());
            assertTrue(cart.getItems().containsAll(List.of(item, item2)));
        }
    }

    @Nested
    class SaveCartTest {

        @Test
        void shouldSaveNewCart() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            Cart cart = new Cart(List.of(item, item2));

            orderService.saveCartAsNotPaidOrder(cart);

            Cart savedCart = orderService.getNotPaidOrderAsCart();
            assertNotNull(savedCart);
            assertFalse(savedCart.isEmpty());
            assertEquals(2, savedCart.getItems().size());
            assertTrue(savedCart.getItems().containsAll(cart.getItems()));
        }

        @Test
        void shouldReplaceItemsInExistingCart() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            OrderEntity orderEntity = new OrderEntity(null, false, List.of(item, item2));
            orderRepository.save(orderEntity);

            Item item3 = new Item(3L, "item3 title", "item3 description", 1220, 2);
            Item item4 = new Item(4L, "item4 title", "item4 description", 520, 1);
            Cart cart = new Cart(List.of(item3, item4));

            orderService.saveCartAsNotPaidOrder(cart);

            Cart savedCart = orderService.getNotPaidOrderAsCart();
            assertNotNull(savedCart);
            assertFalse(savedCart.isEmpty());
            assertEquals(2, savedCart.getItems().size());
            assertTrue(savedCart.getItems().containsAll(cart.getItems()));
        }
    }

    @Nested
    class BuyItemsTest {

        @Test
        void shouldSaveNotPaidOrderAsPaid() {
            Item item = new Item(1L, "item title", "item description", 120, 5);
            Item item2 = new Item(2L, "item2 title", "item2 description", 420, 3);
            OrderEntity orderEntity = new OrderEntity(null, false, List.of(item, item2));
            orderRepository.save(orderEntity);

            Optional<Order> paidOrder = orderService.buyItemsInCart();

            assertTrue(paidOrder.isPresent());
            assertEquals(orderEntity.getId(), paidOrder.get().id());
            assertEquals(2, paidOrder.get().items().size());
            assertTrue(paidOrder.get().items().containsAll(orderEntity.getItems()));
        }

        @Test
        void shouldNotSaveAnythingIfNotPaidOrderNotExists() {
            Optional<Order> paidOrder = orderService.buyItemsInCart();

            assertTrue(paidOrder.isEmpty());
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
        orderRepository.saveAll(List.of(orderEntity1, orderEntity2, orderEntity3));

        Order expectedOrder1 = new Order(orderEntity1.getId(), orderEntity1.getItems());
        Order expectedOrder2 = new Order(orderEntity3.getId(), orderEntity3.getItems());

        List<Order> paidOrders = orderService.getAllPaidOrders();

        assertNotNull(paidOrders);
        assertEquals(2, paidOrders.size());
        assertTrue(paidOrders.containsAll(List.of(expectedOrder1, expectedOrder2)));
    }

    @Test
    void shouldFindPaidOrderById() {
        Item item = new Item(1L, "item title", "item description", 120, 5);
        OrderEntity orderEntity1 = new OrderEntity(null, true, List.of(item));
        OrderEntity saved = orderRepository.save(orderEntity1);

        Optional<Order> paidOrderById = orderService.findPaidOrderById(saved.getId());

        assertTrue(paidOrderById.isPresent());
        assertEquals(saved.getId(), paidOrderById.get().id());
        assertEquals(1, paidOrderById.get().items().size());
        assertTrue(paidOrderById.get().items().contains(item));
    }

    @Test
    void shouldReturnEmptyIfPaidOrderNotFoundById() {
        Item item = new Item(1L, "item title", "item description", 120, 5);
        OrderEntity orderEntity1 = new OrderEntity(null, true, List.of(item));
        OrderEntity saved = orderRepository.save(orderEntity1);

        Optional<Order> paidOrderById = orderService.findPaidOrderById(saved.getId() + 1L);

        assertTrue(paidOrderById.isEmpty());
    }

}