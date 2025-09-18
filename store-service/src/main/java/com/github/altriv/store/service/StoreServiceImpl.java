package com.github.altriv.store.service;

import com.github.altriv.paymentclient.PaymentClient;
import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.Purchase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final ItemService itemService;
    private final OrderService orderService;
    private final TransactionalOperator transactionalOperator;
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
        return orderService.getNotPaidOrderAsCart();
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
        return orderService.getNotPaidOrderAsCart()
                .map(cart -> Purchase.builder().cart(cart).build())
                .flatMap(purchase -> paymentClient.purchase(purchase.generatePurchaseRequest())
                        .onErrorComplete()
                        .map(purchase::processPurchaseResponse)
                        .defaultIfEmpty(purchase.addErrorMessage("Сервис оплаты недоступен. Попробуйте оплатить позже"))
                )
                .flatMap(purchase -> {
                    if (purchase.isSuccess()) {
                        return orderService.saveCartAsPaidOrder().map(purchase::addPaidOrder);
                    } else {
                        return Mono.just(purchase);
                    }
                });
    }

    private void mergeCountFromCartToItem(@NonNull Cart cart, @NonNull Item item) {
        ofNullable(cart.getItem(item.getId()))
                .map(Item::getCount)
                .ifPresent(item::setCount);
    }
}
