package com.github.altriv.store.controller;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.ItemAction;
import com.github.altriv.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final StoreService storeService;

    @GetMapping("/items")
    public String getCartItems(Model model) {
        log.info("Request to get all items in cart");
        Cart cart = storeService.getCart();
        model.addAttribute("items", cart.getItems());
        model.addAttribute("total", cart.getTotalPrice());
        model.addAttribute("empty", cart.isEmpty());
        return "cart";
    }

    @PostMapping("/items/{itemId}")
    public String changeItemCount(@PathVariable("itemId") long itemId,
                                  @RequestParam(value = "action") ItemAction action) {
        log.info("Request to change item count in cart from cart page. Params: itemId= {}, action= {}", itemId, action);
        storeService.changeItemCountInCart(itemId, action);
        return "redirect:/cart/items";
    }
}
