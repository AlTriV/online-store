package com.github.altriv.store.service;

import com.github.altriv.paymentclient.PaymentClient;
import com.github.altriv.paymentclient.domain.BalanceResponse;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.Purchase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.security.Principal;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final ItemService itemService;
    private final OrderService orderService;
    private final TransactionalOperator transactionalOperator;
    private final ReactiveOAuth2AuthorizedClientManager clientManager;
    private final PaymentClient paymentClient;

    @Override
    public Mono<ItemsPage> searchItems(@NonNull String search,
                                       @NonNull ItemSorting sort,
                                       int pageNumber,
                                       int pageSize) {
        return itemService.getItemsPage(search, sort, pageNumber, pageSize)
                .zipWith(orderService.getNotPaidOrderAsCart().defaultIfEmpty(Cart.empty()))
                .map(itemPageAndCart -> {
                    ItemsPage itemsPage = itemPageAndCart.getT1();
                    Cart cart = itemPageAndCart.getT2();
                    itemsPage.getItems().forEach(item -> mergeCountFromCartToItem(cart, item));
                    return itemsPage;
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> changeItemCountInCart(long itemId, @NonNull ItemAction action) {
        return itemService.getItem(itemId)
                .zipWith(orderService.getNotPaidOrderAsCart().defaultIfEmpty(Cart.empty()))
                .map(itemAndCart -> {
                    Item item = itemAndCart.getT1();
                    Cart cart = itemAndCart.getT2();
                    cart.changeItemCountInCart(item, action);
                    return cart;
                })
                .flatMap(orderService::saveCartAsNotPaidOrder)
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Cart> getCart() {
        return orderService.getNotPaidOrderAsCart()
                .flatMap(cart -> getCurrentUsername()
                        .flatMap(username -> requestBalance(username)
                                .onErrorComplete()
                                .map(BalanceResponse::getBalance)
                                .map(cart::putBalance)
                                .defaultIfEmpty(cart)
                        )
                );
    }

    @Override
    public Mono<Item> getItemWithCartCount(long itemId) {
        return itemService.getItem(itemId)
                .zipWith(orderService.getNotPaidOrderAsCart().defaultIfEmpty(Cart.empty()))
                .doOnNext(itemAndCart -> mergeCountFromCartToItem(itemAndCart.getT2(), itemAndCart.getT1()))
                .map(Tuple2::getT1)
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Purchase> buyItemsInCart() {
        return getCurrentUsername()
                .flatMap(username -> orderService.getNotPaidOrderAsCart()
                        .map(cart -> Purchase.builder().cart(cart).username(username).build())
                        .flatMap(this::requestPurchase)
                        .flatMap(purchase -> {
                            if (purchase.isSuccess()) {
                                return orderService.saveCartAsPaidOrder().map(purchase::addPaidOrder);
                            } else {
                                return Mono.just(purchase);
                            }
                        })
                );
    }

    private void mergeCountFromCartToItem(@NonNull Cart cart, @NonNull Item item) {
        ofNullable(cart.getItem(item.getId()))
                .map(Item::getCount)
                .ifPresent(item::setCount);
    }

    private Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName);
    }

    private Mono<Purchase> requestPurchase(Purchase purchase) {
        return requestAccessToken()
                .flatMap(accessToken -> paymentClient.setJwtToken(accessToken)
                        .purchase(purchase.generatePurchaseRequest())
                        .onErrorComplete()
                        .map(purchase::processPurchaseResponse)
                        .defaultIfEmpty(purchase.addErrorMessage("Сервис оплаты недоступен. Попробуйте оплатить позже"))
                );
    }

    private Mono<BalanceResponse> requestBalance(String username) {
        return requestAccessToken().flatMap(accessToken -> paymentClient.setJwtToken(accessToken).getBalance(username));
    }

    private Mono<String> requestAccessToken() {
        return clientManager.authorize(OAuth2AuthorizeRequest
                        .withClientRegistrationId("store-service")
                        .principal("system")
                        .build()
                )
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue);
    }
}
