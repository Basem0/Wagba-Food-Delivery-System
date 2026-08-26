package com.wagba.controller.order;

import com.wagba.dto.order.OrderRequest;
import com.wagba.dto.order.OrderResponse;
import com.wagba.dto.PageResponse;
import com.wagba.dto.order.TrackingResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.OrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<OrderResponse> myOrders(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderService.myOrders(SecurityUtil.getCurrentUserEmail(), pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse myOrder(@PathVariable Long id) {
        return orderService.myOrder(SecurityUtil.getCurrentUserEmail(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancelOrder(SecurityUtil.getCurrentUserEmail(), id);
    }

    @GetMapping("/{id}/tracking")
    public TrackingResponse tracking(@PathVariable Long id) {
        return orderService.tracking(SecurityUtil.getCurrentUserEmail(), id);
    }
}
