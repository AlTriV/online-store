package com.github.altriv.store.model;

public record PageInfo(int pageNumber, int pageSize, boolean hasNext) {
    public boolean hasPrevious() {
        return pageNumber > 1;
    }
}
