package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.Item;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.Order;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final ItemService itemService;
    private final OrderService orderService;

    @Override
    public ItemsPage searchItems(@NonNull String search,
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
                .block();
    }

    @Override
    public void changeItemCountInCart(long itemId, @NonNull ItemAction action) {
        itemService.getItem(itemId)
                .flatMap(item -> orderService.getNotPaidOrderAsCart()
                        .defaultIfEmpty(Cart.empty())
                        .doOnNext(cart -> cart.changeItemCountInCart(item, action))
                        .flatMap(orderService::saveCartAsNotPaidOrder))
                .subscribe();
    }

    @NonNull
    @Override
    public Cart getCart() {
        return orderService.getNotPaidOrderAsCart()
                .blockOptional()
                .orElse(Cart.empty());
    }

    @Override
    public Optional<Item> getItemWithCartCount(long itemId) {
        return itemService.getItem(itemId)
                .zipWith(orderService.getNotPaidOrderAsCart().defaultIfEmpty(Cart.empty()))
                .doOnNext(itemAndCart -> mergeCountFromCartToItem(itemAndCart.getT2(), itemAndCart.getT1()))
                .map(Tuple2::getT1)
                .blockOptional();
    }

    @Override
    public Optional<Order> buyItemsInCart() {
        return orderService.buyItemsInCart().blockOptional();
    }

    private void mergeCountFromCartToItem(@NonNull Cart cart, @NonNull Item item) {
        ofNullable(cart.getItem(item.getId()))
                .map(Item::getCount)
                .ifPresent(item::setCount);
    }
}
