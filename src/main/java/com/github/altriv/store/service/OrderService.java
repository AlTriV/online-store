package com.github.altriv.store.service;

import com.github.altriv.store.model.Cart;

public interface OrderService {

    Cart getNotPaidOrderAsCart();
}
