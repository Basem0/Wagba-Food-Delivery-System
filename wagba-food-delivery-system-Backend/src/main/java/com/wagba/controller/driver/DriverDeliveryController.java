package com.wagba.controller.driver;

import com.wagba.dto.order.DeliveryResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/driver/deliveries")
@PreAuthorize("hasRole('DRIVER')")
public class DriverDeliveryController {

    private final OrderService orderService;

    public DriverDeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/available")
    public List<DeliveryResponse> available() {
        return orderService.availableDeliveries();
    }

    @GetMapping
    public List<DeliveryResponse> myDeliveries() {
        return orderService.myDeliveries(SecurityUtil.getCurrentUserEmail());
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
