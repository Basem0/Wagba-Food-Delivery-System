package com.wagba.controller.driver;

import com.wagba.dto.PageResponse;
import com.wagba.dto.order.DeliveryResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.OrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver/deliveries")
@PreAuthorize("hasRole('DRIVER')")
public class DriverDeliveryController {

    private final OrderService orderService;

    public DriverDeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/available")
    public PageResponse<DeliveryResponse> available(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderService.availableDeliveries(pageable);
    }

    @GetMapping
    public PageResponse<DeliveryResponse> myDeliveries(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderService.myDeliveries(SecurityUtil.getCurrentUserEmail(), pageable);
    }

    @PostMapping("/{id}/accept")
    public DeliveryResponse accept(@PathVariable Long id) {
        return orderService.acceptDelivery(SecurityUtil.getCurrentUserEmail(), id);
    }

    @PostMapping("/{id}/pickup")
    public DeliveryResponse pickup(@PathVariable Long id) {
        return orderService.pickupDelivery(SecurityUtil.getCurrentUserEmail(), id);
    }

    @PostMapping("/{id}/deliver")
    public DeliveryResponse deliver(@PathVariable Long id) {
        return orderService.deliverDelivery(SecurityUtil.getCurrentUserEmail(), id);
    }
}
