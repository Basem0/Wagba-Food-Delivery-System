package com.wagba.controller.order;

import com.wagba.dto.order.DriverTrackingInfo;
import com.wagba.dto.order.OrderRequest;
import com.wagba.dto.order.OrderResponse;
import com.wagba.dto.PageResponse;
import com.wagba.dto.order.TrackingResponse;
import com.wagba.entity.Delivery;
import com.wagba.entity.Driver;
import com.wagba.entity.Order;
import com.wagba.entity.User;
import com.wagba.repository.DeliveryRepository;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.UserRepository;
import com.wagba.security.SecurityUtil;
import com.wagba.service.OrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService,
                           OrderRepository orderRepository,
                           DeliveryRepository deliveryRepository,
                           DriverRepository driverRepository,
                           UserRepository userRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.deliveryRepository = deliveryRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
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
        String email = SecurityUtil.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Order order = orderRepository.findByCustomerAndId(customer, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        Delivery delivery = deliveryRepository.findByOrder(order).orElse(null);
        DriverTrackingInfo driverInfo = null;
        if (delivery != null && delivery.getDriver() != null) {
            User d = delivery.getDriver();
            Driver drv = driverRepository.findByUser(d).orElse(null);
            if (drv != null) {
                driverInfo = new DriverTrackingInfo(
                        drv.getId(),
                        d.getName(),
                        drv.getPhoneNumber(),
                        drv.getVehicleType(),
                        drv.getVehicleNumber(),
                        drv.getLatitude(),
                        drv.getLongitude(),
                        drv.getLocationUpdatedAt() != null ? drv.getLocationUpdatedAt().toString() : null
                );
            }
        }

        return new TrackingResponse(
                order.getId(),
                order.getStatus().name(),
                delivery != null ? delivery.getStatus().name() : null,
                order.getRestaurant() != null ? order.getRestaurant().getName() : null,
                driverInfo,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                order.getCustomerLatitude(),
                order.getCustomerLongitude()
        );
    }
}
