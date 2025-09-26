package com.github.altriv.store.controller;

import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    @Value("${store.view.itemsInRow}")
    private final int itemsInRow = 3;

    private final ItemService itemService;

    @GetMapping
    public Mono<String> getItems(@RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                                 @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                 Model model) {
        log.info("ADMIN. Request to get items with params: pageNumber = {}, pageSize = {}", pageNumber, pageSize);
        return itemService.getItemsPage("", ItemSorting.NO, pageNumber, pageSize)
                .switchIfEmpty(Mono.just(new ItemsPage(List.of(), new PageInfo(pageNumber, pageSize, false))))
                .doOnNext(itemsPage -> {
                    model.addAttribute("paging", itemsPage.getPageInfo());
                    model.addAttribute("items", itemsPage.getItemRows(itemsInRow));
                })
                .map(itemsPage -> "admin-items");
    }

    @GetMapping("/items")
    public Mono<String> getSaveItemPage() {
        return Mono.just("admin-add-item");
    }

    @PostMapping(path = "/items")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> saveItem(@RequestPart(name = "title") String title,
                                 @RequestPart(name = "description") String description,
                                 @RequestPart(name = "price") String price,
                                 @RequestPart(name = "image") byte[] image) {
        log.info("ADMIN. Request to save item. Params: title = {}, description = {}, price = {}", title, description, price);
        return itemService.saveItem(title, description, Integer.parseInt(price), image)
                .then(Mono.just("redirect:/admin"));
    }

    @PostMapping("/items/{itemId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> deleteItem(@PathVariable("itemId") String itemId) {
        log.info("ADMIN. Request to delete item with id: {}", itemId);
        return itemService.deleteItem(Long.parseLong(itemId))
                .then(Mono.just("redirect:/admin"));
    }
}
