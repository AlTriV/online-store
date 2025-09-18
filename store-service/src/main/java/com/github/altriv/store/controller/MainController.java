package com.github.altriv.store.controller;

import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    @Value("${store.view.itemsInRow}")
    private final int itemsInRow = 3;

    private final StoreService storeService;

    @GetMapping
    public Mono<String> start() {
        return Mono.just("redirect:/main/items");
    }

    @GetMapping(path = "/main/items")
    public Mono<String> getItems(@RequestParam(name = "search", defaultValue = "") String search,
                                 @RequestParam(name = "sort", defaultValue = "NO") ItemSorting sort,
                                 @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                                 @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                 Model model) {
        log.info("Request to get items. Params: search= '{}', sort= {}, pageNumber= {}, pageSize= {}", search, sort, pageNumber, pageSize);
        return storeService.searchItems(search, sort, pageNumber, pageSize)
                .doOnNext(itemsPage -> {
                    model.addAttribute("paging", itemsPage.getPageInfo());
                    model.addAttribute("items", itemsPage.getItemRows(itemsInRow));
                })
                .map(itemsPage -> "main");
    }

    @PostMapping(value = "/main/items/{itemId}")
    public Mono<String> changeItemCount(@PathVariable("itemId") long itemId,
                                        @RequestPart("action") String action) {
        log.info("Request to change item count in cart from main page. Params: itemId= {}, action= {}", itemId, action);
        return storeService.changeItemCountInCart(itemId, ItemAction.valueOf(action))
                .then(Mono.just("redirect:/main/items"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<String>> handleCustomException(IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.badRequest().body(ex.getMessage()));
    }
}
