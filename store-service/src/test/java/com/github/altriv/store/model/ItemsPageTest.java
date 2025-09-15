package com.github.altriv.store.model;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ItemsPageTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 7})
    void shouldReturnItemRows(int numberOfItemsInRow) {
        int numberOfItems = 6;
        ArrayList<Item> items = new ArrayList<>();
        IntStream.range(0, numberOfItems).forEach(i -> items.add(mock(Item.class)));

        int expectedNumberOfRows = Math.ceilDiv(numberOfItems, numberOfItemsInRow);

        int expectedNumberOfItemsInLastRow =
                numberOfItems % numberOfItemsInRow == 0 ?
                        numberOfItemsInRow : numberOfItems % numberOfItemsInRow;

        ItemsPage itemsPage = new ItemsPage(items, null);

        List<List<Item>> itemRows = itemsPage.getItemRows(numberOfItemsInRow);

        assertNotNull(itemRows);
        assertEquals(expectedNumberOfRows, itemRows.size());
        IntStream.range(0, expectedNumberOfRows - 1)
                .forEach(i -> assertEquals(numberOfItemsInRow, itemRows.get(i).size()));
        assertEquals(expectedNumberOfItemsInLastRow, itemRows.get(expectedNumberOfRows - 1).size());
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 0})
    void shouldThrowRuntimeException(int numberOfItemsInRow) {
        int numberOfItems = 6;
        ArrayList<Item> items = new ArrayList<>();
        IntStream.range(0, numberOfItems).forEach(i -> items.add(mock(Item.class)));

        ItemsPage itemsPage = new ItemsPage(items, null);

        assertThrows(IllegalArgumentException.class, () -> itemsPage.getItemRows(numberOfItemsInRow));
    }

}