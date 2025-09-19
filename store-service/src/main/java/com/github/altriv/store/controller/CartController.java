package com.github.altriv.store.controller;

import com.github.altriv.store.model.Cart;
import com.github.altriv.store.model.ItemAction;
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
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final StoreService storeService;

    @GetMapping("/items")
    public Mono<String> getCartItems(Model model) {
        log.info("Request to get all items in cart");
        return storeService.getCart()
                .doOnNext(cart -> {
                    model.addAttribute("errorMessage", cart.getPaymentUnavailableReason());
                    model.addAttribute("items", cart.getItems());
                    model.addAttribute("total", cart.getTotalPrice());
                    model.addAttribute("canPurchase", !cart.isEmpty() && cart.isPossibleToPayForCart());
                })
                .map(cart -> "cart");
    }

    @PostMapping("/items/{itemId}")
    public Mono<String> changeItemCount(@PathVariable("itemId") long itemId,
                                        @RequestPart(value = "action") String action) {
        log.info("Request to change item count in cart from cart page. Params: itemId= {}, action= {}", itemId, action);
        return storeService.changeItemCountInCart(itemId, ItemAction.valueOf(action))
                .then(Mono.just("redirect:/cart/items"));
    }

    @PostMapping("/buy")
    public Mono<String> buyItems(Model model) {
        log.info("Request to buy items in cart");
        return storeService.buyItemsInCart()
                .map(purchase -> {
                    if (purchase.isSuccess()) {
                        return String.format("redirect:/orders/%d?newOrder=true", purchase.getPaidOrder().id());
                    } else {
                        log.info("Purchase failed. Return cart page");
                        Cart cart = purchase.getCart();
                        model.addAttribute("errorMessage", purchase.getErrorMessage());
                        model.addAttribute("items", cart.getItems());
                        model.addAttribute("total", cart.getTotalPrice());
                        model.addAttribute("canPurchase", false);
                        return "cart";
                    }
                });
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<String>> handleCustomException(IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.badRequest().body(ex.getMessage()));
    }
}
