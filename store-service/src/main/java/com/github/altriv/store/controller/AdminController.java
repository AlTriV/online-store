package com.github.altriv.store.controller;

import com.github.altriv.store.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ItemService itemService;

    @GetMapping("/items")
    public Mono<String> getSaveItemPage() {
        return Mono.just("add-item");
    }

    @PostMapping(path = "/items")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> saveItem(@RequestPart(name = "title") String title,
                                 @RequestPart(name = "description") String description,
                                 @RequestPart(name = "price") String price,
                                 @RequestPart(name = "image") byte[] image) {
        log.info("ADMIN. Request to save item. Params: title = {}, description = {}, price = {}", title, description, price);
        return itemService.saveItem(title, description, Integer.parseInt(price), image)
                .then(Mono.just("redirect:/main/items"));
    }

    @PostMapping("/items/{itemId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> deleteItem(@PathVariable("itemId") String itemId) {
        log.info("ADMIN. Request to delete item with id: {}", itemId);
        return itemService.deleteItem(Long.parseLong(itemId))
                .then(Mono.just("redirect:/main/items"));
    }
}
