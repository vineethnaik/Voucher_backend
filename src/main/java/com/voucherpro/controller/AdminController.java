package com.voucherpro.controller;

import com.voucherpro.dto.OrderResponse;
import com.voucherpro.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;

    public AdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/pending")
    public List<OrderResponse> getPendingOrders() {
        return orderService.getPendingOrders();
    }

    @RequestMapping(value = "/{orderId}/approve", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public OrderResponse approveOrder(@PathVariable String orderId) {
        return orderService.approveOrder(orderId);
    }

    @RequestMapping(value = "/{orderId}/reject", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public OrderResponse rejectOrder(@PathVariable String orderId) {
        return orderService.rejectOrder(orderId);
    }
}
