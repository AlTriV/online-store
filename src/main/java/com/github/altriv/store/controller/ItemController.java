package com.github.altriv.store.controller;

import com.github.altriv.store.model.Item;
import com.github.altriv.store.service.ItemService;
import com.github.altriv.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final StoreService storeService;

    @GetMapping("/{itemId}")
    public String getItem(@PathVariable long itemId,
                          Model model) {
        log.info("Request to get item with id {}", itemId);
        Optional<Item> foundItem = storeService.getItemWithCartCount(itemId);
        foundItem.ifPresentOrElse(
                item -> model.addAttribute("item", item),
                () -> log.warn("Item with id {} not found. Redirect to main page", itemId)
        );
        return foundItem.isPresent() ? "item" : "redirect:/main/items";
    }

    @GetMapping("/{itemId}/image")
    public @ResponseBody byte[] getItemImage(@PathVariable("itemId") long itemId) {
        return itemService.getItemImage(itemId);
    }
}
