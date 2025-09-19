package com.github.altriv.store.service;

import com.github.altriv.paymentclient.PaymentClient;
import com.github.altriv.paymentclient.domain.BalanceResponse;
import com.github.altriv.paymentclient.domain.PurchaseRequest;
import com.github.altriv.paymentclient.domain.PurchaseResponse;
import com.github.altriv.store.entity.OrderEntity;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.Order;
import com.github.altriv.store.model.PageInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StoreServiceImpl.class)
class StoreServiceImplTest {

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PaymentClient paymentClient;

    @MockitoBean
    private TransactionalOperator transactionalOperator;

    @Autowired
    private StoreServiceImpl storeService;

    @Nested
    class SearchItems {

        @ParameterizedTest
        @CsvSource({", NO", "'', "})
        void shouldThrowNPEIfSearchStringOrSortingIsNull(String searchString, ItemSorting itemSorting) {
            assertThrows(
                    NullPointerException.class,
                    () -> storeService.searchItems(searchString, itemSorting, 1, 10).block()
            );
        }

        @Test
        void shouldReturnItemsPage() {
            String search = "item";
            ItemSorting sort = ItemSorting.NO;
            int pageNumber = 1;
            int pageSize = 10;

            Item cartItem1 = new Item(1L, "item1", "item1 description", 1200, 5);
            Item cartItem2 = new Item(2L, "item2", "item2 description", 1300, 3);
            Item item1 = new Item(1L, "item1", "item1 description", 1200, 0);
            Item item2 = new Item(2L, "item2", "item2 description", 1300, 0);
            Item item3 = new Item(3L, "item3", "item3 description", 1500, 0);
            Item item4 = new Item(4L, "item4", "item4 description", 1500, 0);

            Cart cart = new Cart(List.of(cartItem1, cartItem2));
            ItemsPage itemsPage = new ItemsPage(
                    List.of(item1, item2, item3, item4),
                    new PageInfo(pageNumber, pageSize,
                            false)
            );
            when(itemService.getItemsPage(search, sort, pageNumber, pageSize)).thenReturn(Mono.just(itemsPage));
            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(transactionalOperator.transactional(ArgumentMatchers.<Mono<ItemsPage>>any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            storeService.searchItems(search, sort, pageNumber, pageSize)
                    .doOnNext(resultItemsPage -> {
                        assertNotNull(resultItemsPage);
                        assertEquals(itemsPage.getPageInfo(), resultItemsPage.getPageInfo());
                        assertEquals(4, resultItemsPage.getItems().size());
                        assertTrue(resultItemsPage.getItems().containsAll(List.of(cartItem1, cartItem2, item3, item4)));
                    })
                    .block();
            verify(itemService, times(1)).getItemsPage(search, sort, pageNumber, pageSize);
            verify(orderService, times(1)).getNotPaidOrderAsCart();
        }
    }

    @Nested
    class ChangeItemCountInCartTest {

        @BeforeEach
        void initTransactionalOperator() {
            when(transactionalOperator.transactional(ArgumentMatchers.<Mono<?>>any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @ParameterizedTest
        @EnumSource(ItemAction.class)
        void shouldDoNothingIfItemNotFound(ItemAction action) {
            long itemId = 1L;
            when(itemService.getItem(itemId)).thenReturn(Mono.empty());
            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.empty());

            storeService.changeItemCountInCart(itemId, action).block();

            verify(itemService, times(1)).getItem(itemId);
            verify(orderService, times(1)).getNotPaidOrderAsCart();
            verifyNoMoreInteractions(itemService);
            verifyNoMoreInteractions(orderService);
        }

        @ParameterizedTest
        @EnumSource(ItemAction.class)
        void shouldCallItemCountChangeIfItemFound(ItemAction action) {
            long itemId = 1L;
            Item item = new Item(1L, "item1", "item1 description", 1200, 0);
            Item itemInCart = new Item(1L, "item1", "item1 description", 1200, 5);
            Cart cart = spy(new Cart(List.of(itemInCart)));
            when(itemService.getItem(itemId)).thenReturn(Mono.just(item));
            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(orderService.saveCartAsNotPaidOrder(cart)).thenReturn(Mono.empty());

            storeService.changeItemCountInCart(itemId, action).block();

            verify(itemService, times(1)).getItem(itemId);
            verify(orderService, times(1)).getNotPaidOrderAsCart();
            verify(cart, times(1)).changeItemCountInCart(item, action);
            verify(orderService, times(1)).saveCartAsNotPaidOrder(cart);
            verifyNoMoreInteractions(itemService);
        }
    }

    @Nested
    class GetItemWithCartCount {

        @BeforeEach
        void initTransactionalOperator() {
            when(transactionalOperator.transactional(ArgumentMatchers.<Mono<OrderEntity>>any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void shouldReturnEmptyIfItemNotFound() {
            long itemId = 1L;
            Cart cart = Cart.empty();

            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(itemService.getItem(itemId)).thenReturn(Mono.empty());

            storeService.getItemWithCartCount(itemId)
                    .doOnNext(Assertions::assertNull)
                    .block();
        }

        @Test
        void shouldReturnItemWithCartCount() {
            long itemId = 1L;
            Item item = new Item(1L, "item1", "item1 description", 1200, 0);
            Item itemInCart = new Item(1L, "item1", "item1 description", 1200, 5);
            Cart cart = new Cart(List.of(itemInCart));

            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(itemService.getItem(itemId)).thenReturn(Mono.just(item));

            storeService.getItemWithCartCount(itemId)
                    .doOnNext(resultItem -> {
                        assertNotNull(resultItem);
                        assertEquals(item.getId(), resultItem.getId());
                        assertEquals(item.getTitle(), resultItem.getTitle());
                        assertEquals(item.getDescription(), resultItem.getDescription());
                        assertEquals(item.getPrice(), resultItem.getPrice());
                        assertEquals(itemInCart.getCount(), resultItem.getCount());
                    })
                    .block();
        }
    }

    @Nested
    class BuyItemsInCartTest {

        @Test
        void shouldReturnFailedPurchaseIfPaymentServiceNotAvailable() {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            PurchaseRequest purchaseRequest = new PurchaseRequest().requestId(UUID.randomUUID()).price((long) cart.getTotalPrice());
            PurchaseRequestMatcher purchaseRequestMatcher = PurchaseRequestMatcher.matcherFor(purchaseRequest);

            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(paymentClient.purchase(argThat(purchaseRequestMatcher)))
                    .thenReturn(Mono.error(new RuntimeException("Payment service down")));

            storeService.buyItemsInCart()
                    .doOnNext(purchase -> {
                        assertNotNull(purchase);
                        assertFalse(purchase.isSuccess());
                        assertEquals(cart, purchase.getCart());
                        assertEquals("Сервис оплаты недоступен. Попробуйте оплатить позже", purchase.getErrorMessage());
                        assertNull(purchase.getPaidOrder());
                    })
                    .block();
            verify(orderService, times(1)).getNotPaidOrderAsCart();
            verify(paymentClient, times(1)).purchase(argThat(purchaseRequestMatcher));
            verifyNoMoreInteractions(orderService);
        }

        @Test
        void shouldReturnFailedPurchaseIfPaymentUnsuccessful() {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            UUID requestUuid = UUID.randomUUID();
            String errorMessage = "Недостаточно средств";
            PurchaseRequest purchaseRequest = new PurchaseRequest().requestId(requestUuid).price((long) cart.getTotalPrice());
            PurchaseResponse purchaseResponse = new PurchaseResponse().purchaseResult(false).requestId(requestUuid).errorMessage(errorMessage);

            PurchaseRequestMatcher purchaseRequestMatcher = PurchaseRequestMatcher.matcherFor(purchaseRequest);

            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(paymentClient.purchase(argThat(purchaseRequestMatcher))).thenReturn(Mono.just(purchaseResponse));


            storeService.buyItemsInCart()
                    .doOnNext(purchase -> {
                        assertNotNull(purchase);
                        assertFalse(purchase.isSuccess());
                        assertEquals(cart, purchase.getCart());
                        assertEquals(errorMessage, purchase.getErrorMessage());
                        assertNull(purchase.getPaidOrder());
                    })
                    .block();
            verify(orderService, times(1)).getNotPaidOrderAsCart();
            verify(paymentClient, times(1)).purchase(argThat(purchaseRequestMatcher));
            verifyNoMoreInteractions(orderService);
        }

        @Test
        void shouldReturnSuccessPurchaseIfPaymentSuccess() {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            Order paidOrder = new Order(1L, List.of(item, item2));
            UUID requestUuid = UUID.randomUUID();
            PurchaseRequest purchaseRequest = new PurchaseRequest().requestId(requestUuid).price((long) cart.getTotalPrice());
            PurchaseResponse purchaseResponse = new PurchaseResponse().purchaseResult(true).requestId(requestUuid);

            PurchaseRequestMatcher purchaseRequestMatcher = PurchaseRequestMatcher.matcherFor(purchaseRequest);

            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(paymentClient.purchase(argThat(purchaseRequestMatcher))).thenReturn(Mono.just(purchaseResponse));
            when(orderService.saveCartAsPaidOrder()).thenReturn(Mono.just(paidOrder));

            storeService.buyItemsInCart()
                    .doOnNext(purchase -> {
                        assertNotNull(purchase);
                        assertTrue(purchase.isSuccess());
                        assertEquals(cart, purchase.getCart());
                        assertEquals(paidOrder, purchase.getPaidOrder());
                        assertNull(purchase.getErrorMessage());
                    })
                    .block();
            verify(orderService, times(1)).getNotPaidOrderAsCart();
            verify(paymentClient, times(1)).purchase(argThat(purchaseRequestMatcher));
            verify(orderService, times(1)).saveCartAsPaidOrder();
        }
    }

    @Nested
    class GetCartTets {

        @Test
        void shouldReturnCartWithBalance() {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));

            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(paymentClient.getBalance()).thenReturn(Mono.error(new RuntimeException("Payment service down")));

            storeService.getCart()
                    .doOnNext(resultCart -> {
                        assertNotNull(resultCart);
                        assertEquals(2, cart.getItems().size());
                        assertTrue(cart.getItems().containsAll(List.of(item, item2)));
                        assertFalse(cart.isPossibleToPayForCart());
                        assertNotNull(cart.getPaymentUnavailableReason());
                        assertEquals("Нет информации о балансе, возможность оплаты временно недоступна", cart.getPaymentUnavailableReason());
                    })
                    .block();
            verify(orderService, times(1)).getNotPaidOrderAsCart();
            verify(paymentClient, times(1)).getBalance();
        }

        @Test
        void shouldReturnCartWithoutBalance() {
            Item item = new Item(1L, "title", "description", 1000, 2);
            Item item2 = new Item(2L, "title2", "description2", 2000, 1);
            Cart cart = new Cart(List.of(item, item2));
            BalanceResponse balanceResponse = new BalanceResponse().balance(12345L);

            when(orderService.getNotPaidOrderAsCart()).thenReturn(Mono.just(cart));
            when(paymentClient.getBalance()).thenReturn(Mono.just(balanceResponse));

            storeService.getCart()
                    .doOnNext(resultCart -> {
                        assertNotNull(resultCart);
                        assertEquals(2, cart.getItems().size());
                        assertTrue(cart.getItems().containsAll(List.of(item, item2)));
                        assertTrue(cart.isPossibleToPayForCart());
                        assertNull(cart.getPaymentUnavailableReason());
                    })
                    .block();
            verify(orderService, times(1)).getNotPaidOrderAsCart();
            verify(paymentClient, times(1)).getBalance();
        }
    }

    record PurchaseRequestMatcher(PurchaseRequest baseRequest) implements ArgumentMatcher<PurchaseRequest> {
        @Override
        public boolean matches(PurchaseRequest argumentRequest) {
            if (isNull(baseRequest)) {
                return isNull(argumentRequest);
            }
            if (isNull(argumentRequest)) {
                return false;
            }
            return baseRequest.getPrice().equals(argumentRequest.getPrice());
        }

        static PurchaseRequestMatcher matcherFor(PurchaseRequest purchaseRequest) {
            return new PurchaseRequestMatcher(purchaseRequest);
        }
    }
}