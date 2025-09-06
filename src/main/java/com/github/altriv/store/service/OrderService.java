package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;
import lombok.NonNull;

public interface OrderService {

    Cart getNotPaidOrderAsCart();

    void saveCartAsNotPaidOrder(@NonNull Cart cart);
}
