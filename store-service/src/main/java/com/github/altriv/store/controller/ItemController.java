package com.github.altriv.store.controller;

import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.service.ItemService;
import com.github.altriv.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final StoreService storeService;

    @GetMapping("/{itemId}")
    public Mono<String> getItem(@PathVariable long itemId,
                                Model model) {
        log.info("Request to get item with id {}", itemId);
        return storeService.getItemWithCartCount(itemId)
                .map(item -> {
                    model.addAttribute("item", item);
                    return "item";
                })
                .defaultIfEmpty("redirect:/main/items");
    }

    @GetMapping("/{itemId}/image")
    public @ResponseBody Mono<byte[]> getItemImage(@PathVariable("itemId") long itemId) {
        return itemService.getItemImage(itemId);
    }

    @PostMapping("/{itemId}")
    public Mono<String> changeItemCount(@PathVariable("itemId") long itemId,
                                        @RequestPart(value = "action") String action) {
        log.info("Request to change item count in cart from item page. Params: itemId= {}, action= {}", itemId, action);
        return storeService.changeItemCountInCart(itemId, ItemAction.valueOf(action))
                .then(Mono.just("redirect:/items/" + itemId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<String>> handleCustomException(IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.badRequest().body(ex.getMessage()));
    }
}
