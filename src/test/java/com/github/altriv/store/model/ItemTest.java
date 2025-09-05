package com.github.altriv.store.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void increaseCountTest(int numberOfIncrease) {
        Item item = new Item(1L, "title", "description", 1000, 0);

        IntStream.range(0, numberOfIncrease).forEach(i -> item.increaseCount());

        assertEquals(numberOfIncrease, item.getCount());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void decreaseCountTest(int numberOfDecrease) {
        Item item = new Item(1L, "title", "description", 1000, numberOfDecrease);

        IntStream.range(0, numberOfDecrease).forEach(i -> item.decreaseCount());

        assertEquals(0, item.getCount());
    }

}