package com.github.altriv.store.controller;

import com.github.altriv.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Mono<String> getOrders(Model model) {
        log.info("Request to get all paid orders");
        return orderService.getAllPaidOrders()
                .collectList()
                .doOnNext(paidOrders -> model.addAttribute("orders", paidOrders))
                .map(paidOrders -> "orders");
    }

    @GetMapping("/{orderId}")
    public Mono<String> getOrder(@PathVariable("orderId") long orderId,
                                 @RequestParam(name = "newOrder", defaultValue = "false") boolean newOrder,
                                 Model model) {
        log.info("Request to get paid order by id= {}", orderId);
        return orderService.findPaidOrderById(orderId)
                .doOnNext(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                })
                .map(order -> "order")
                .defaultIfEmpty("redirect:/orders");
    }
}
