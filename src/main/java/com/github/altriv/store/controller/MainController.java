package com.github.altriv.store.controller;

import com.github.altriv.store.model.ItemSorting;
import com.github.altriv.store.model.ItemsPage;
import com.github.altriv.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class MainController {

    @Value("${store.view.itemsInRow}")
    private final int itemsInRow = 3;

    private final StoreService storeService;

    @GetMapping
    public String start() {
        return "redirect:/main/items";
    }

    @GetMapping("/main/items")
    public String getItems(@RequestParam(name = "search", defaultValue = "") String search,
                           @RequestParam(name = "sort", defaultValue = "NO") ItemSorting sort,
                           @RequestParam(name = "pageNumber", defaultValue = "1") int pageNumber,
                           @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                           Model model) {
        log.info("Request to get items. Params: search= '{}', sort= {}, pageNumber= {}, pageSize= {}", search, sort, pageNumber, pageSize);

        ItemsPage itemsPage = storeService.searchItems(search, sort, pageNumber, pageSize);

        model.addAttribute("paging", itemsPage.getPageInfo());
        model.addAttribute("items", itemsPage.getItemRows(itemsInRow));

        return "main";
    }
}
