package com.github.altriv.store.controller;

import com.github.altriv.store.model.Order;
import com.github.altriv.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public String getOrders(Model model) {
        log.info("Request to get all paid orders");
        List<Order> paidOrders = orderService.getAllPaidOrders();
        model.addAttribute("orders", paidOrders);
        return "orders";
    }

    @GetMapping("/{orderId}")
    public String getOrder(@PathVariable("orderId") long orderId,
                           @RequestParam(name = "newOrder", defaultValue = "false") boolean newOrder,
                           Model model) {
        log.info("Request to get paid order by id= {}", orderId);
        Optional<Order> foundOrder = orderService.findPaidOrderById(orderId);
        foundOrder.ifPresentOrElse(
                order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                },
                () -> log.warn("No order found with id={}. Redirect to all orders page", orderId)
        );
        return foundOrder.isPresent() ? "order" : "redirect:/orders";
    }
}
