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
import com.wagba.entity.Driver;
import com.wagba.entity.Food;
import com.wagba.entity.Order;
import com.wagba.entity.OrderItem;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.DeliveryStatus;
import com.wagba.entity.enums.OrderStatus;
import com.wagba.entity.enums.PaymentMethod;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.repository.AddressRepository;
import com.wagba.repository.CartItemRepository;
import com.wagba.repository.CartRepository;
import com.wagba.repository.DeliveryRepository;
import com.wagba.repository.DriverRepository;
import com.wagba.repository.OrderItemRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.ReviewRepository;
import com.wagba.repository.UserRepository;
import com.wagba.dto.PageResponse;
import com.wagba.realtime.RealtimeNotification;
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

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final CouponService couponService;
    private final RealtimeService realtime;
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;
    private final WalletService walletService;

    @Value("${wagba.delivery.fee:15}")
    private BigDecimal defaultDeliveryFee;

    @Value("${wagba.delivery.driver-share:0.85}")
    private BigDecimal driverShare;

    /** Wagba's cut of the food revenue on each delivered order. */
    @Value("${wagba.platform.fee-rate:0.10}")
    private BigDecimal platformFeeRate;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        DeliveryRepository deliveryRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        AddressRepository addressRepository,
                         RestaurantRepository restaurantRepository,
                          UserRepository userRepository,
                          DriverRepository driverRepository,
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
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
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

    private static final double NEAR_RADIUS_METERS = 300.0;

    private void assertDriverNear(Driver drv, Double targetLat, Double targetLng, String context) {
        if (drv == null || drv.getLatitude() == null || drv.getLongitude() == null) {
            throw new RuntimeException("Enable and allow location access on your device before you can " + context + ".");
        }
        if (targetLat == null || targetLng == null) {
            return;
        }
        double meters = distanceMeters(drv.getLatitude(), drv.getLongitude(), targetLat, targetLng);
        if (meters > NEAR_RADIUS_METERS) {
            throw new RuntimeException("You must be at the location to " + context + " (you are " + (int) meters + "m away, max allowed " + (int) NEAR_RADIUS_METERS + "m).");
        }
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Restaurant ownRestaurant(User owner) {
        return restaurantRepository.findByOwner(owner)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) return PaymentMethod.CARD;
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported payment method: " + raw);
        }
    }

    /**
     * Picks the delivery address for an order. A saved address is used as-is: it
     * used to be overwritten with the request's coordinates, which silently moved
     * the pin on every past order that referenced it.
     */
    private Address resolveAddress(User customer, OrderRequest request) {
        if (request.getAddressId() != null) {
            Address saved = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new RuntimeException("Address not found"));
            if (saved.getUser() == null || !saved.getUser().getId().equals(customer.getId())) {
                throw new RuntimeException("Address does not belong to you");
            }
            // Only fill in coordinates that were missing; never move an existing pin.
            if (saved.getLatitude() == null && request.getLatitude() != null) {
                saved.setLatitude(request.getLatitude());
                saved.setLongitude(request.getLongitude());
                return addressRepository.save(saved);
            }
            return saved;
        }

        if (isBlank(request.getCity()) && isBlank(request.getStreet()) && isBlank(request.getDetails())) {
            throw new RuntimeException("A delivery address is required");
        }

        Address address = new Address();
        address.setUser(customer);
        address.setCity(trimOrNull(request.getCity()));
        address.setStreet(trimOrNull(request.getStreet()));
        address.setBuildingNumber(trimOrNull(request.getBuildingNumber()));
        address.setApartment(trimOrNull(request.getApartment()));
        address.setDetails(trimOrNull(request.getDetails()));
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        return addressRepository.save(address);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    // ---------- Customer ----------

    /**
     * Price actually charged for a food item: the offer price when one is set and
     * genuinely lower, otherwise the list price.
     */
    static BigDecimal effectivePrice(Food food) {
        BigDecimal price = food.getPrice();
        BigDecimal offer = food.getDiscountPrice();
        if (price == null) {
            throw new RuntimeException("\"" + food.getName() + "\" has no price set and cannot be ordered");
        }
        if (offer != null && offer.compareTo(BigDecimal.ZERO) > 0 && offer.compareTo(price) < 0) {
            return offer;
        }
        return price;
    }

    public OrderResponse checkout(String email, OrderRequest request) {
        User customer = currentUser(email);
        Cart cart = cartRepository.findByUser(customer)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Your cart is empty");
        }

        Restaurant restaurant = cart.getItems().get(0).getFood().getCategory().getRestaurant();
        for (CartItem ci : cart.getItems()) {
            if (!ci.getFood().getCategory().getRestaurant().getId().equals(restaurant.getId())) {
                throw new RuntimeException("All cart items must belong to the same restaurant");
            }
        }

        // The restaurant may have been suspended, or an item taken off the menu,
        // between adding to the cart and checking out.
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new RuntimeException(restaurant.getName() + " is not accepting orders right now");
        }
        for (CartItem ci : cart.getItems()) {
            Food food = ci.getFood();
            if (!food.isAvailable()) {
                throw new RuntimeException("\"" + food.getName() + "\" is no longer available. Please remove it from your cart.");
            }
            if (ci.getQuantity() == null || ci.getQuantity() < 1) {
                throw new RuntimeException("Invalid quantity for \"" + food.getName() + "\"");
            }
        }

        Address address = resolveAddress(customer, request);

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(address);
        order.setStatus(OrderStatus.PENDING);
        Double lat = request.getLatitude() != null ? request.getLatitude() : address.getLatitude();
        Double lng = request.getLongitude() != null ? request.getLongitude() : address.getLongitude();
        order.setCustomerLatitude(lat);
        order.setCustomerLongitude(lng);
        order.setPaymentMethod(parsePaymentMethod(request.getPaymentMethod()));

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            BigDecimal unitPrice = effectivePrice(ci.getFood());
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setFood(ci.getFood());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(unitPrice);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
            items.add(oi);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal minOrder = restaurant.getMinOrderTotal();
        if (minOrder != null && subtotal.compareTo(minOrder) < 0) {
            throw new RuntimeException("Minimum order for " + restaurant.getName() + " is "
                    + minOrder.setScale(2, RoundingMode.HALF_UP) + " EGP (your items total "
                    + subtotal + " EGP)");
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            couponCode = couponCode.trim();
            discount = couponService.applyCoupon(couponCode, customer, subtotal);
        } else {
            couponCode = null;
        }
        if (discount == null) discount = BigDecimal.ZERO;
        // Never let a coupon discount more than the items are worth.
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        discount = discount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal fee = restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : defaultDeliveryFee;
        fee = fee.setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setDeliveryFee(fee);
        order.setDiscountAmount(discount);
        order.setCouponCode(couponCode);
        // The delivery fee used to be dropped here, so drivers were paid out of
        // money the customer was never charged.
        order.setTotalPrice(subtotal.subtract(discount).add(fee).setScale(2, RoundingMode.HALF_UP));
        order = orderRepository.save(order);
        orderItemRepository.saveAll(items);
        order.setItems(items);

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setStatus(DeliveryStatus.AVAILABLE);
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
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new RuntimeException("Order #" + id + " is " + order.getStatus()
                    + " and can no longer be cancelled. Please contact the restaurant.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        deliveryRepository.findByOrder(order).ifPresent(d -> {
            d.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.save(d);
        });
        // Give the coupon back so a cancelled order doesn't burn the customer's use.
        if (order.getCouponCode() != null) {
            try {
                couponService.releaseCoupon(order.getCouponCode(), customer);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(OrderService.class)
                        .warn("Could not release coupon {} for order {}: {}", order.getCouponCode(), id, e.getMessage());
            }
        }
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
        assertTransition(order, OrderStatus.ACCEPTED);
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
        assertTransition(order, OrderStatus.REJECTED);
        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);
        deliveryRepository.findByOrder(order).ifPresent(d -> {
            d.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.save(d);
        });
        if (order.getCouponCode() != null) {
            try {
                couponService.releaseCoupon(order.getCouponCode(), order.getCustomer());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(OrderService.class)
                        .warn("Could not release coupon {} for order {}: {}", order.getCouponCode(), id, e.getMessage());
            }
        }
        notifyUser(order.getCustomer().getEmail(), "ORDER_REJECTED", "Order rejected",
                "Your order #" + id + " was rejected by the restaurant", id);
        return toResponse(order);
    }

    private static final java.util.Set<OrderStatus> OWNER_PROGRESSABLE =
            java.util.Set.of(OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY);

    /**
     * Legal next states for an order. Enforces the README's rule that status
     * transitions follow business rules - previously the kitchen could jump
     * ACCEPTED straight to OUT_FOR_DELIVERY, or move READY back to PREPARING.
     */
    private static final java.util.Map<OrderStatus, java.util.Set<OrderStatus>> ALLOWED_TRANSITIONS =
            java.util.Map.of(
                    OrderStatus.PENDING, java.util.Set.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.CANCELLED),
                    OrderStatus.ACCEPTED, java.util.Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
                    OrderStatus.PREPARING, java.util.Set.of(OrderStatus.READY),
                    OrderStatus.READY, java.util.Set.of(OrderStatus.OUT_FOR_DELIVERY),
                    OrderStatus.OUT_FOR_DELIVERY, java.util.Set.of(OrderStatus.DELIVERED),
                    OrderStatus.DELIVERED, java.util.Set.of(),
                    OrderStatus.CANCELLED, java.util.Set.of(),
                    OrderStatus.REJECTED, java.util.Set.of()
            );

    private void assertTransition(Order order, OrderStatus target) {
        OrderStatus current = order.getStatus();
        if (current == target) {
            throw new RuntimeException("Order #" + order.getId() + " is already " + target);
        }
        java.util.Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, java.util.Set.of());
        if (!allowed.contains(target)) {
            if (allowed.isEmpty()) {
                throw new RuntimeException("Order #" + order.getId() + " is " + current + " and can no longer change");
            }
            throw new RuntimeException("Cannot move order #" + order.getId() + " from " + current
                    + " to " + target + ". Next step must be " + allowed.iterator().next() + ".");
        }
    }

    public OrderResponse updateRestaurantOrderStatus(String email, Long id, OrderStatus status) {
        if (!OWNER_PROGRESSABLE.contains(status)) {
            throw new RuntimeException("Status " + status + " is not allowed for restaurant update");
        }
        User owner = currentUser(email);
        Restaurant restaurant = ownRestaurant(owner);
        Order order = orderRepository.findByRestaurantAndId(restaurant, id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        assertTransition(order, status);
        order.setStatus(status);
        orderRepository.save(order);
        if (status == OrderStatus.OUT_FOR_DELIVERY) {
            // Owner handing the food to the driver == picked up, so the driver can deliver.
            // Never jump the delivery straight to OUT_FOR_DELIVERY (that broke "Deliver").
            deliveryRepository.findByOrder(order).ifPresent(d -> {
                if (d.getStatus() == DeliveryStatus.ACCEPTED) {
                    d.setStatus(DeliveryStatus.PICKED_UP);
                    d.setPickedUpAt(LocalDateTime.now());
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
        Restaurant restaurant = delivery.getOrder().getRestaurant();
        Driver drv = driverRepository.findByUser(driver).orElse(null);
        assertDriverNear(drv, restaurant.getLatitude(), restaurant.getLongitude(),
                "the restaurant to pick up the order");
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
        Order o = delivery.getOrder();
        Double custLat = o.getCustomerLatitude();
        Double custLng = o.getCustomerLongitude();
        if (custLat == null && o.getDeliveryAddress() != null) {
            custLat = o.getDeliveryAddress().getLatitude();
            custLng = o.getDeliveryAddress().getLongitude();
        }
        Driver drv = driverRepository.findByUser(driver).orElse(null);
        assertDriverNear(drv, custLat, custLng, "the customer to deliver the order");
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        o.setStatus(OrderStatus.DELIVERED);
        // Cash is collected at the door, so delivery is the moment it is paid.
        if (o.getPaymentMethod() == PaymentMethod.CASH && !o.isPaid()) {
            o.setPaid(true);
            o.setPaidAt(LocalDateTime.now());
            o.setPaymentReference("CASH");
        }
        orderRepository.save(o);
        deliveryRepository.save(delivery);

        notifyUser(o.getCustomer().getEmail(), "ORDER_DELIVERED", "Delivered",
                "Your order #" + o.getId() + " was delivered", o.getId());
        notifyUser(o.getRestaurant().getOwner().getEmail(), "ORDER_DELIVERED", "Delivered",
                "Order #" + o.getId() + " was delivered", o.getId());
        if (delivery.getDriver() != null) {
            notifyUser(delivery.getDriver().getEmail(), "DELIVERY_EARNING", "Delivery complete",
                    "You earned " + delivery.getEarning() + " on order #" + o.getId(), o.getId());
        }
        settlePayouts(o, delivery);
        return toDeliveryResponse(delivery);
    }

    /**
     * Credits the restaurant and the driver once an order is both delivered and
     * paid. Payouts used to fire for any CARD order even if the payment never
     * completed, and never fired for cash.
     */
    private void settlePayouts(Order o, Delivery delivery) {
        if (!o.isPaid()) {
            org.slf4j.LoggerFactory.getLogger(OrderService.class)
                    .warn("Order {} delivered but not paid - skipping payouts", o.getId());
            return;
        }
        BigDecimal fee = o.getDeliveryFee() != null ? o.getDeliveryFee()
                : (delivery.getFee() != null ? delivery.getFee() : BigDecimal.ZERO);
        // The delivery fee belongs to the courier, so the restaurant's cut is
        // taken from the food revenue only.
        BigDecimal foodRevenue = o.getTotalPrice().subtract(fee).max(BigDecimal.ZERO);
        BigDecimal restaurantPayout = foodRevenue
                .multiply(BigDecimal.ONE.subtract(platformFeeRate))
                .setScale(2, RoundingMode.HALF_UP);

        if (restaurantPayout.compareTo(BigDecimal.ZERO) > 0) {
            walletService.credit(o.getRestaurant().getOwner(),
                    restaurantPayout, "Payout for order #" + o.getId(), "ORDER-" + o.getId());
        }
        if (delivery.getDriver() != null && delivery.getEarning() != null
                && delivery.getEarning().compareTo(BigDecimal.ZERO) > 0) {
            walletService.credit(delivery.getDriver(),
                    delivery.getEarning(), "Earning for order #" + o.getId(), "ORDER-" + o.getId());
        }
    }

    // ---------- Payments ----------

    /**
     * Records a completed card payment. Called by PaymentController once Stripe
     * confirms (or immediately, in dev mode). Idempotent.
     */
    public OrderResponse markPaid(String email, Long orderId, String paymentReference) {
        User customer = currentUser(email);
        Order order = orderRepository.findByCustomerAndId(customer, orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
            throw new RuntimeException("Order #" + orderId + " is " + order.getStatus() + " and cannot be paid");
        }
        if (order.isPaid()) {
            return toResponse(order);
        }
        order.setPaid(true);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentReference(paymentReference);
        orderRepository.save(order);

        notifyUser(order.getRestaurant().getOwner().getEmail(), "ORDER_PAID", "Payment received",
                "Order #" + orderId + " has been paid", orderId);
        notifyUser(customer.getEmail(), "ORDER_PAID", "Payment confirmed",
                "We received your payment for order #" + orderId, orderId);
        return toResponse(order);
    }

    // ---------- Admin ----------

    public PageResponse<OrderResponse> allOrders(OrderStatus status, Pageable pageable) {
        Page<Order> p = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        Page<OrderResponse> op = p.map(this::toResponse);
        return new PageResponse<>(op.getContent(), op.getNumber(), op.getSize(), op.getTotalElements(), op.getTotalPages());
    }

    public long countAll() {
        return orderRepository.count();
    }

    /** Gross value of delivered orders, for the admin dashboard. */
    public BigDecimal totalRevenue() {
        BigDecimal sum = orderRepository.sumTotalByStatus(OrderStatus.DELIVERED);
        return (sum == null ? BigDecimal.ZERO : sum).setScale(2, RoundingMode.HALF_UP);
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
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (OrderItem oi : order.getItems()) {
            BigDecimal unit = oi.getUnitPrice() != null ? oi.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal line = unit.multiply(BigDecimal.valueOf(oi.getQuantity()));
            itemsTotal = itemsTotal.add(line);
            items.add(new OrderItemResponse(
                    oi.getFood().getId(),
                    oi.getFood().getName(),
                    oi.getQuantity(),
                    unit,
                    line
            ));
        }
        Address addr = order.getDeliveryAddress();
        AddressResponse addressResponse = addr == null ? null : new AddressResponse(
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
        // Orders created before the breakdown columns existed have no stored
        // subtotal, so fall back to summing the line items.
        BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : itemsTotal;
        User cust = order.getCustomer();
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                subtotal,
                order.getDeliveryFee(),
                order.getTotalPrice(),
                order.getDiscountAmount(),
                order.getCouponCode(),
                addressResponse,
                items,
                deliveryStatus,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                cust != null ? cust.getName() : null,
                cust != null ? cust.getPhone() : null,
                order.getCustomerLatitude(),
                order.getCustomerLongitude(),
                reviewed,
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null,
                order.isPaid()
        );
    }

    private DeliveryResponse toDeliveryResponse(Delivery delivery) {
        Order o = delivery.getOrder();
        Restaurant r = o != null ? o.getRestaurant() : null;
        User cust = o != null ? o.getCustomer() : null;
        Address addr = o != null ? o.getDeliveryAddress() : null;
        Double custLat = o != null ? o.getCustomerLatitude() : null;
        Double custLng = o != null ? o.getCustomerLongitude() : null;
        if (custLat == null && addr != null) { custLat = addr.getLatitude(); custLng = addr.getLongitude(); }
        return new DeliveryResponse(
                delivery.getId(),
                o != null ? o.getId() : null,
                delivery.getStatus().name(),
                delivery.getDriver() != null ? delivery.getDriver().getId() : null,
                delivery.getAcceptedAt() != null ? delivery.getAcceptedAt().toString() : null,
                delivery.getPickedUpAt() != null ? delivery.getPickedUpAt().toString() : null,
                delivery.getDeliveredAt() != null ? delivery.getDeliveredAt().toString() : null,
                delivery.getFee(),
                delivery.getEarning(),
                r != null ? r.getName() : null,
                r != null ? r.getLatitude() : null,
                r != null ? r.getLongitude() : null,
                null,
                cust != null ? cust.getName() : null,
                custLat,
                custLng,
                addr != null ? addrLine(addr) : null
        );
    }

    private String addrLine(Address a) {
        if (a == null) return null;
        StringBuilder sb = new StringBuilder();
        if (a.getStreet() != null) sb.append(a.getStreet());
        if (a.getBuildingNumber() != null) sb.append(sb.length() == 0 ? "" : ", ").append("Bldg ").append(a.getBuildingNumber());
        if (a.getApartment() != null) sb.append(", Apt ").append(a.getApartment());
        if (a.getCity() != null) sb.append(sb.length() == 0 ? "" : ", ").append(a.getCity());
        return sb.length() == 0 ? null : sb.toString();
    }
}
