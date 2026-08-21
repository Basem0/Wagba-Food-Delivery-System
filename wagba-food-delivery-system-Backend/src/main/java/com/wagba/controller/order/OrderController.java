package com.wagba.controller.order;

import com.wagba.dto.order.OrderRequest;
import com.wagba.dto.order.OrderResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@RequestBody OrderRequest request) {
        return orderService.checkout(SecurityUtil.getCurrentUserEmail(), request);
    }

    @GetMapping
    public List<OrderResponse> myOrders() {
        return orderService.myOrders(SecurityUtil.getCurrentUserEmail());
    }

    @GetMapping("/{id}")
    public OrderResponse myOrder(@PathVariable Long id) {
        return orderService.myOrder(SecurityUtil.getCurrentUserEmail(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancelOrder(SecurityUtil.getCurrentUserEmail(), id);
    }
}
