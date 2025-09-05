package com.github.altriv.store.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @Test
    void emptyCartTest() {
        Cart emptyCart = Cart.empty();

        assertNotNull(emptyCart);
        List<Item> items = emptyCart.getItems();
        assertTrue(items.isEmpty());
    }

    @Test
    void shouldAddNewItem() {
        Cart cart = Cart.empty();
        Item item = new Item(1L, "title", "description", 1000, 0);

        cart.addItem(item);

        List<Item> items = cart.getItems();
        assertNotNull(items);
        assertEquals(1, items.size());
        assertTrue(items.contains(item));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void shouldIncreateCountOfExistingItem(int iterations) {
        Item item = new Item(1L, "title", "description", 1000, 0);

        Cart cart = new Cart(List.of(item));

        IntStream.range(0, iterations).forEach(i -> cart.addItem(item));

        List<Item> items = cart.getItems();
        assertNotNull(items);
        assertEquals(1, items.size());
        assertTrue(items.contains(item));
        assertEquals(iterations, items.getFirst().getCount());
    }

    @Test
    void shouldReturnItems() {
        Cart cart = Cart.empty();
        Item item = new Item(1L, "title", "description", 1000, 0);
        Item item2 = new Item(2L, "title2", "description2", 2000, 0);
        cart.addItem(item);
        cart.addItem(item2);

        List<Item> items = cart.getItems();

        assertNotNull(items);
        assertEquals(2, items.size());
        assertTrue(items.containsAll(List.of(item, item2)));
    }

    @Test
    void shouldReturnNullIfNoItemWithGivenId() {
        Cart cart = Cart.empty();
        Item item = new Item(1L, "title", "description", 1000, 0);
        cart.addItem(item);

        Item found = cart.getItem(2L);

        assertNull(found);
    }

    @Test
    void shouldReturnItemIfExists() {
        Cart cart = Cart.empty();
        Item item = new Item(1L, "title", "description", 1000, 0);
        cart.addItem(item);

        Item found = cart.getItem(1L);

        assertNotNull(found);
        assertEquals(item, found);
    }

}