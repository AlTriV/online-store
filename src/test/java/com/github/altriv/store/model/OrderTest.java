package com.github.altriv.store.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldReturnTotalPrice() {
        int price1 = 1000;
        int price2 = 2000;
        Item item = new Item(1L, "title", "description", price1, 2);
        Item item2 = new Item(2L, "title2", "description2", price2, 1);
        Order order = new Order(1L, List.of(item, item2));
        int expectedPrice = 2 * price1 + price2;

        assertEquals(expectedPrice, order.totalPrice());
    }

    @Test
    void shouldReturnZeroIfOrderIsEmpty() {
        Order order = new Order(1L, List.of());

        assertEquals(0, order.totalPrice());
    }
}