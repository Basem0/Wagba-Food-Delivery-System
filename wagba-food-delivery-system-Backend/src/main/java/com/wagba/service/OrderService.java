package com.wagba.service;

import com.wagba.dto.order.AddressResponse;
import com.wagba.dto.order.DeliveryResponse;
import com.wagba.dto.order.OrderItemResponse;
import com.wagba.dto.order.OrderRequest;
import com.wagba.dto.order.OrderResponse;
import com.wagba.entity.Address;
import com.wagba.entity.Cart;
import com.wagba.entity.CartItem;
import com.wagba.entity.Delivery;
import com.wagba.entity.Food;
import com.wagba.entity.Order;
import com.wagba.entity.OrderItem;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.DeliveryStatus;
import com.wagba.entity.enums.OrderStatus;
import com.wagba.entity.enums.PaymentMethod;
import com.wagba.repository.AddressRepository;
import com.wagba.repository.CartItemRepository;
import com.wagba.repository.CartRepository;
import com.wagba.repository.DeliveryRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.OrderItemRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.ReviewRepository;
import com.wagba.repository.UserRepository;
import com.wagba.dto.PageResponse;
import com.wagba.realtime.RealtimeNotification;
import com.wagba.service.CouponService;
import com.wagba.service.RealtimeService;
import com.wagba.service.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final FoodRepository foodRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final RealtimeService realtime;
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;
    private final WalletService walletService;

    @Value("${wagba.delivery.fee:15}")
    private BigDecimal defaultDeliveryFee;

    @Value("${wagba.delivery.driver-share:0.85}")
    private BigDecimal driverShare;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        DeliveryRepository deliveryRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        AddressRepository addressRepository,
                        FoodRepository foodRepository,
                        RestaurantRepository restaurantRepository,
                         UserRepository userRepository,
                         CouponService couponService,
                         RealtimeService realtime,
                         NotificationService notificationService,
                         ReviewRepository reviewRepository,
                         WalletService walletService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.foodRepository = foodRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.couponService = couponService;
        this.realtime = realtime;
        this.notificationService = notificationService;
        this.reviewRepository = reviewRepository;
        this.walletService = walletService;
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Restaurant ownRestaurant(User owner) {
        return restaurantRepository.findByOwner(owner)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    // ---------- Customer ----------

    public OrderResponse checkout(String email, OrderRequest request) {
        User customer = currentUser(email);
        Cart cart = cartRepository.findByUser(customer)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Restaurant restaurant = cart.getItems().get(0).getFood().getCategory().getRestaurant();
        for (CartItem ci : cart.getItems()) {
            if (!ci.getFood().getCategory().getRestaurant().getId().equals(restaurant.getId())) {
                throw new RuntimeException("All cart items must belong to the same restaurant");
            }
        }

        Address address;
        if (request.getAddressId() != null) {
            address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new RuntimeException("Address not found"));
            if (!address.getUser().getId().equals(customer.getId())) {
                throw new RuntimeException("Address does not belong to you");
            }
        } else {
            address = new Address();
            address.setUser(customer);
            address.setCity(request.getCity());
            address.setStreet(request.getStreet());
            address.setBuildingNumber(request.getBuildingNumber());
            address.setApartment(request.getApartment());
            address.setDetails(request.getDetails());
        }
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address = addressRepository.save(address);

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(address);
        order.setStatus(OrderStatus.PENDING);
        if (request.getLatitude() != null) order.setCustomerLatitude(request.getLatitude());
        if (request.getLongitude() != null) order.setCustomerLongitude(request.getLongitude());
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setFood(ci.getFood());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getFood().getPrice());
            subtotal = subtotal.add(ci.getFood().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            items.add(oi);
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            discount = couponService.applyCoupon(couponCode, customer, subtotal);
        }
        order.setDiscountAmount(discount);
        order.setCouponCode(couponCode);
        order.setTotalPrice(subtotal.subtract(discount));
        order = orderRepository.save(order);
        orderItemRepository.saveAll(items);
        order.setItems(items);

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setStatus(DeliveryStatus.AVAILABLE);
        BigDecimal fee = restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : defaultDeliveryFee;
        BigDecimal earning = fee.multiply(driverShare).setScale(2, RoundingMode.HALF_UP);
        delivery.setFee(fee);
        delivery.setEarning(earning);
        deliveryRepository.save(delivery);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        String ownerEmail = restaurant.getOwner().getEmail();
        String customerEmail = customer.getEmail();
        notifyUser(ownerEmail, "NEW_ORDER", "New order #" + order.getId(),
                "New order from " + customer.getName(), order.getId());
        notifyUser(customerEmail, "ORDER_PLACED", "Order placed",
                "Your order #" + order.getId() + " has been placed", order.getId());
        realtime.toTopic("/topic/driver/available",
                new RealtimeNotification("NEW_DELIVERY", "New delivery available",
                        "Order #" + order.getId(), order.getId()));

        return toResponse(order);
    }

    public PageResponse<OrderResponse> myOrders(String email, Pageable pageable) {
        User customer = currentUser(email);
        Page<Order> p = orderRepository.findByCustomer(customer, pageable);
        Page<OrderResponse> op = p.map(this::toResponse);
        return new PageResponse<>(op.getContent(), op.getNumber(), op.getSize(), op.getTotalElements(), op.getTotalPages());
    }

    public OrderResponse myOrder(String email, Long id) {
        User customer = currentUser(email);
        Order order = orderRepository.findByCustomerAndId(customer, id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(order);
    }

    public OrderResponse cancelOrder(String email, Long id) {
        User customer = currentUser(email);
        Order order = orderRepository.findByCustomerAndId(customer, id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        deliveryRepository.findByOrder(order).ifPresent(d -> {
            d.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.save(d);
        });
        notifyUser(order.getRestaurant().getOwner().getEmail(), "ORDER_CANCELLED", "Order cancelled",
                "Order #" + id + " was cancelled by the customer", id);
        return toResponse(order);
    }

    // ---------- Restaurant owner ----------

    public PageResponse<OrderResponse> restaurantOrders(String email, Pageable pageable) {
        User owner = currentUser(email);
        Restaurant restaurant = ownRestaurant(owner);
        Page<Order> p = orderRepository.findByRestaurant(restaurant, pageable);
        Page<OrderResponse> op = p.map(this::toResponse);
        return new PageResponse<>(op.getContent(), op.getNumber(), op.getSize(), op.getTotalElements(), op.getTotalPages());
    }

    public OrderResponse restaurantOrder(String email, Long id) {
        User owner = currentUser(email);
        Restaurant restaurant = ownRestaurant(owner);
        Order order = orderRepository.findByRestaurantAndId(restaurant, id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(order);
    }

    public OrderResponse acceptOrder(String email, Long id) {
        User owner = currentUser(email);
        Restaurant restaurant = ownRestaurant(owner);
        Order order = orderRepository.findByRestaurantAndId(restaurant, id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order already processed");
        }
        order.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(order);
        notifyUser(order.getCustomer().getEmail(), "ORDER_ACCEPTED", "Order accepted",
                "Your order #" + id + " was accepted by the restaurant", id);
        return toResponse(order);
    }

    public OrderResponse rejectOrder(String email, Long id) {
        User owner = currentUser(email);
        Restaurant restaurant = ownRestaurant(owner);
        Order order = orderRepository.findByRestaurantAndId(restaurant, id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order already processed");
        }
        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);
        deliveryRepository.findByOrder(order).ifPresent(d -> {
            d.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.save(d);
        });
        notifyUser(order.getCustomer().getEmail(), "ORDER_REJECTED", "Order rejected",
                "Your order #" + id + " was rejected by the restaurant", id);
        return toResponse(order);
    }

    private static final java.util.Set<OrderStatus> OWNER_PROGRESSABLE =
            java.util.Set.of(OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY);

    public OrderResponse updateRestaurantOrderStatus(String email, Long id, OrderStatus status) {
        if (!OWNER_PROGRESSABLE.contains(status)) {
            throw new RuntimeException("Status " + status + " is not allowed for restaurant update");
        }
        User owner = currentUser(email);
        Restaurant restaurant = ownRestaurant(owner);
        Order order = orderRepository.findByRestaurantAndId(restaurant, id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.ACCEPTED
                && order.getStatus() != OrderStatus.PREPARING
                && order.getStatus() != OrderStatus.READY) {
            throw new RuntimeException("Order cannot be advanced from " + order.getStatus());
        }
        order.setStatus(status);
        orderRepository.save(order);
        if (status == OrderStatus.OUT_FOR_DELIVERY) {
            // Owner handing the food to the driver == picked up, so the driver can deliver.
            // Never jump the delivery straight to OUT_FOR_DELIVERY (that broke "Deliver").
            deliveryRepository.findByOrder(order).ifPresent(d -> {
                if (d.getStatus() == DeliveryStatus.ACCEPTED) {
                    d.setStatus(DeliveryStatus.PICKED_UP);
                    deliveryRepository.save(d);
                }
            });
        }
        notifyUser(order.getCustomer().getEmail(), "ORDER_STATUS", "Order status: " + status,
                "Your order #" + id + " is now " + status, id);
        return toResponse(order);
    }

    // ---------- Driver ----------

    public PageResponse<DeliveryResponse> availableDeliveries(Pageable pageable) {
        Page<Delivery> p = deliveryRepository.findByDriverIsNullAndStatus(DeliveryStatus.AVAILABLE, pageable);
        Page<DeliveryResponse> dp = p.map(this::toDeliveryResponse);
        return new PageResponse<>(dp.getContent(), dp.getNumber(), dp.getSize(), dp.getTotalElements(), dp.getTotalPages());
    }

    public PageResponse<DeliveryResponse> myDeliveries(String email, Pageable pageable) {
        User driver = currentUser(email);
        Page<Delivery> p = deliveryRepository.findByDriver(driver, pageable);
        Page<DeliveryResponse> dp = p.map(this::toDeliveryResponse);
        return new PageResponse<>(dp.getContent(), dp.getNumber(), dp.getSize(), dp.getTotalElements(), dp.getTotalPages());
    }

    public DeliveryResponse acceptDelivery(String email, Long deliveryId) {
        User driver = currentUser(email);
        List<DeliveryStatus> activeStatuses = List.of(DeliveryStatus.ACCEPTED, DeliveryStatus.PICKED_UP, DeliveryStatus.OUT_FOR_DELIVERY);
        boolean hasActive = deliveryRepository.findByDriver(driver).stream()
                .anyMatch(d -> activeStatuses.contains(d.getStatus()));
        if (hasActive) {
            throw new RuntimeException("You already have an active delivery. Complete it before accepting a new one.");
        }
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        if (delivery.getStatus() != DeliveryStatus.AVAILABLE || delivery.getDriver() != null) {
            throw new RuntimeException("Delivery is not available");
        }
        delivery.setDriver(driver);
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setAcceptedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        Order o = delivery.getOrder();
        notifyUser(o.getRestaurant().getOwner().getEmail(), "DRIVER_ASSIGNED",
                "Driver assigned", "A driver accepted delivery for order #" + o.getId(), o.getId());
        notifyUser(o.getCustomer().getEmail(), "DRIVER_ASSIGNED",
                "Driver assigned", "A driver is on the way for order #" + o.getId(), o.getId());
        realtime.toTopic("/topic/driver/available",
                new RealtimeNotification("DELIVERY_TAKEN", "Delivery taken",
                        "Order #" + o.getId(), o.getId()));
        return toDeliveryResponse(delivery);
    }

    public DeliveryResponse pickupDelivery(String email, Long deliveryId) {
        User driver = currentUser(email);
        Delivery delivery = deliveryRepository.findByDriverAndId(driver, deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        if (delivery.getStatus() != DeliveryStatus.ACCEPTED) {
            throw new RuntimeException("Delivery must be accepted first");
        }
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setPickedUpAt(LocalDateTime.now());
        delivery.getOrder().setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(delivery.getOrder());
        deliveryRepository.save(delivery);
        notifyUser(delivery.getOrder().getCustomer().getEmail(), "ORDER_STATUS",
                "Picked up", "Your order #" + delivery.getOrder().getId() + " was picked up", delivery.getOrder().getId());
        return toDeliveryResponse(delivery);
    }

    public DeliveryResponse deliverDelivery(String email, Long deliveryId) {
        User driver = currentUser(email);
        Delivery delivery = deliveryRepository.findByDriverAndId(driver, deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        if (delivery.getStatus() != DeliveryStatus.PICKED_UP) {
            throw new RuntimeException("Delivery must be picked up first");
        }
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.getOrder().setStatus(OrderStatus.DELIVERED);
        orderRepository.save(delivery.getOrder());
        deliveryRepository.save(delivery);

        Order o = delivery.getOrder();
        notifyUser(o.getCustomer().getEmail(), "ORDER_DELIVERED", "Delivered",
                "Your order #" + o.getId() + " was delivered", o.getId());
        notifyUser(o.getRestaurant().getOwner().getEmail(), "ORDER_DELIVERED", "Delivered",
                "Order #" + o.getId() + " was delivered", o.getId());
        if (delivery.getDriver() != null) {
            notifyUser(delivery.getDriver().getEmail(), "DELIVERY_EARNING", "Delivery complete",
                    "You earned " + delivery.getEarning() + " on order #" + o.getId(), o.getId());
        }
        if (o.getPaymentMethod() == PaymentMethod.CARD) {
            BigDecimal platformFeeRate = new BigDecimal("0.10");
            BigDecimal restaurantPayout = o.getTotalPrice()
                    .multiply(BigDecimal.ONE.subtract(platformFeeRate))
                    .setScale(2, RoundingMode.HALF_UP);
            walletService.credit(o.getRestaurant().getOwner(),
                    restaurantPayout, "Payout for order #" + o.getId(), "ORDER-" + o.getId());
            if (delivery.getDriver() != null && delivery.getEarning() != null) {
                walletService.credit(delivery.getDriver(),
                        delivery.getEarning(), "Earning for order #" + o.getId(), "ORDER-" + o.getId());
            }
        }
        return toDeliveryResponse(delivery);
    }

    // ---------- Realtime ----------

    private void notifyUser(String email, String type, String title, String message, Long orderId) {
        if (email == null) return;
        try {
            RealtimeNotification n = new RealtimeNotification(type, title, message, orderId);
            realtime.notifyUser(email, n);
            notificationService.save(email, n);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(OrderService.class)
                    .warn("Realtime notify failed for {}: {}", email, e.getMessage());
        }
    }

    // ---------- Mapping ----------

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = new ArrayList<>();
        for (OrderItem oi : order.getItems()) {
            items.add(new OrderItemResponse(
                    oi.getFood().getId(),
                    oi.getFood().getName(),
                    oi.getQuantity(),
                    oi.getUnitPrice(),
                    oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity()))
            ));
        }
        Address addr = order.getDeliveryAddress();
        AddressResponse addressResponse = new AddressResponse(
                addr.getId(),
                addr.getCity(),
                addr.getStreet(),
                addr.getBuildingNumber(),
                addr.getApartment(),
                addr.getDetails(),
                addr.getLatitude(),
                addr.getLongitude()
        );
        String deliveryStatus = deliveryRepository.findByOrder(order)
                .map(d -> d.getStatus().name()).orElse(null);
        boolean reviewed = reviewRepository.existsByOrderId(order.getId());
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getTotalPrice(),
                order.getDiscountAmount(),
                order.getCouponCode(),
                addressResponse,
                items,
                deliveryStatus,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                order.getCustomer() != null ? order.getCustomer().getName() : null,
                order.getCustomerLatitude(),
                order.getCustomerLongitude(),
                reviewed,
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null
        );
    }

    private DeliveryResponse toDeliveryResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getOrder().getId(),
                delivery.getStatus().name(),
                delivery.getDriver() != null ? delivery.getDriver().getId() : null,
                delivery.getAcceptedAt() != null ? delivery.getAcceptedAt().toString() : null,
                delivery.getPickedUpAt() != null ? delivery.getPickedUpAt().toString() : null,
                delivery.getDeliveredAt() != null ? delivery.getDeliveredAt().toString() : null,
                delivery.getFee(),
                delivery.getEarning()
        );
    }
}
