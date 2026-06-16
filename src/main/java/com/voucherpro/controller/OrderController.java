package com.voucherpro.controller;

import com.voucherpro.dto.CreateOrderRequest;
import com.voucherpro.dto.OrderResponse;
import com.voucherpro.dto.SubmitUtrRequest;
import com.voucherpro.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(authentication.getName(), request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderDetails(
            Authentication authentication,
            @PathVariable String orderId
    ) {
        return orderService.getOrderDetails(authentication.getName(), orderId);
    }

    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders(Authentication authentication) {
        return orderService.getMyOrders(authentication.getName());
    }

    @PostMapping("/{orderId}/submit-utr")
    public OrderResponse submitUtr(
            Authentication authentication,
            @PathVariable String orderId,
            @Valid @RequestBody SubmitUtrRequest request
    ) {
        return orderService.submitUtr(authentication.getName(), orderId, request);
    }
}
