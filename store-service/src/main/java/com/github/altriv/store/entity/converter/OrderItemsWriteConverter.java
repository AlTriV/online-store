package com.github.altriv.store.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.altriv.store.model.Item;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

@Slf4j
public record OrderItemsWriteConverter(ObjectMapper objectMapper) implements Converter<List<Item>, Json> {

    @Override
    public Json convert(List<Item> source) {
        try {
            byte[] convertedItems = objectMapper.writeValueAsBytes(source);
            return Json.of(convertedItems);
        } catch (JsonProcessingException e) {
            log.error("Error on converting order's item list to json. List = {}", source, e);
            return Json.of("{}");
        }
    }
}
