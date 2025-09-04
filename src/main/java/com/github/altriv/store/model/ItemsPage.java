package com.github.altriv.store.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.ListUtils;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class ItemsPage {

    @NonNull
    private List<Item> items;
    private PageInfo pageInfo;

    public List<List<Item>> getItemRows(int itemsInRow) {
        return ListUtils.partition(items, itemsInRow);
    }
}
