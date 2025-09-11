package com.github.altriv.store.controller;

import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.model.PageInfo;
import com.github.altriv.store.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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
    public String getItems(@RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                           @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                           Model model) {
        log.info("ADMIN. Request to get items with params: pageNumber = {}, pageSize = {}", pageNumber, pageSize);
        ItemsPage itemsPage = itemService.getItemsPage("", ItemSorting.NO, pageNumber, pageSize)
                .switchIfEmpty(Mono.just(new ItemsPage(List.of(), new PageInfo(pageNumber, pageSize, false))))
                .block();

        model.addAttribute("paging", itemsPage.getPageInfo());
        model.addAttribute("items", itemsPage.getItemRows(itemsInRow));

        return "admin-items";
    }

    @GetMapping("/items")
    public String getSaveItemPage() {
        return "admin-add-item";
    }

    @PostMapping(path = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String saveItem(@RequestParam(name = "title", defaultValue = "") String title,
                           @RequestParam(name = "description", defaultValue = "") String description,
                           @RequestParam(name = "price", defaultValue = "0") Integer price,
                           @RequestPart(name = "image") byte[] image) {
        log.info("ADMIN. Request to save item. Params: title = {}, description = {}, price = {}", title, description, price);
        itemService.saveItem(title, description, price, image).subscribe();
        return "redirect:/admin";
    }

    @PostMapping("/items/{itemId}/delete")
    public String deleteItem(@PathVariable("itemId") String itemId) {
        log.info("ADMIN. Request to delete item with id: {}", itemId);
        itemService.deleteItem(Long.parseLong(itemId)).subscribe();
        return "redirect:/admin";
    }
}
