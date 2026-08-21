package com.wagba.controller.restaurant;

import com.wagba.dto.order.OrderResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurant-owner/orders")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOwnerOrderController {

    private final OrderService orderService;

    public RestaurantOwnerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> orders() {
        return orderService.restaurantOrders(SecurityUtil.getCurrentUserEmail());
    }

    @GetMapping("/{id}")
    public OrderResponse order(@PathVariable Long id) {
        return orderService.restaurantOrder(SecurityUtil.getCurrentUserEmail(), id);
    }

    @PostMapping("/{id}/accept")
    public OrderResponse accept(@PathVariable Long id) {
        return orderService.acceptOrder(SecurityUtil.getCurrentUserEmail(), id);
    }

    @PostMapping("/{id}/reject")
    public OrderResponse reject(@PathVariable Long id) {
        return orderService.rejectOrder(SecurityUtil.getCurrentUserEmail(), id);
    }
}
