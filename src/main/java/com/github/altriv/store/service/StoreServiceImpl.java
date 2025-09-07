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
        ItemsPage itemsPage = itemService.getItemsPage(search, sort, pageNumber, pageSize);
        Cart cart = orderService.getNotPaidOrderAsCart();
        itemsPage.getItems().forEach(item -> mergeCountFromCartToItem(cart, item));
        return itemsPage;
    }

    @Override
    public void changeItemCountInCart(long itemId, @NonNull ItemAction action) {
        Cart cart = orderService.getNotPaidOrderAsCart();
        itemService.getItem(itemId).ifPresent(item -> {
            cart.changeItemCountInCart(item, action);
            orderService.saveCartAsNotPaidOrder(cart);
        });
    }

    @NonNull
    @Override
    public Cart getCart() {
        return orderService.getNotPaidOrderAsCart();
    }

    @Override
    public Optional<Item> getItemWithCartCount(long itemId) {
        Cart cart = orderService.getNotPaidOrderAsCart();
        return itemService.getItem(itemId).map(item -> {
            mergeCountFromCartToItem(cart, item);
            return item;
        });
    }

    @Override
    public Optional<Order> buyItemsInCart() {
        return orderService.buyItemsInCart();
    }

    private void mergeCountFromCartToItem(@NonNull Cart cart, @NonNull Item item) {
        ofNullable(cart.getItem(item.getId()))
                .map(Item::getCount)
                .ifPresent(item::setCount);
    }
}
