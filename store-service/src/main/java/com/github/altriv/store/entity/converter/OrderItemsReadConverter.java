package com.github.altriv.store.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.altriv.store.model.Item;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public record OrderItemsReadConverter(ObjectMapper objectMapper) implements Converter<Json, List<Item>> {

    @Override
    public List<Item> convert(Json source) {
        try {
            byte[] jsonBytes = source.asArray();
            return objectMapper.readValue(jsonBytes, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Error on converting json to order's item list. Json = {}", source, e);
            return new ArrayList<>();
        }
    }
}
