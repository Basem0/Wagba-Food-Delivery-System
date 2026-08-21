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
import com.wagba.repository.AddressRepository;
import com.wagba.repository.CartItemRepository;
import com.wagba.repository.CartRepository;
import com.wagba.repository.DeliveryRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.OrderItemRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import com.wagba.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final FoodRepository foodRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        DeliveryRepository deliveryRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        AddressRepository addressRepository,
                        FoodRepository foodRepository,
                        RestaurantRepository restaurantRepository,
                        UserRepository userRepository,
                        CouponService couponService) {
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
            address = addressRepository.save(address);
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(address);
        order.setStatus(OrderStatus.PENDING);

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
        deliveryRepository.save(delivery);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        return toResponse(order);
    }

    public List<OrderResponse> myOrders(String email) {
        User customer = currentUser(email);
        return orderRepository.findByCustomer(customer).stream().map(this::toResponse).toList();
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
        return toResponse(order);
    }

    // ---------- Restaurant owner ----------

    public List<OrderResponse> restaurantOrders(String email) {
        User owner = currentUser(email);
        Restaurant restaurant = ownRestaurant(owner);
        return orderRepository.findByRestaurant(restaurant).stream().map(this::toResponse).toList();
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
        return toResponse(order);
    }

    // ---------- Driver ----------

    public List<DeliveryResponse> availableDeliveries() {
        return deliveryRepository.findByDriverIsNullAndStatus(DeliveryStatus.AVAILABLE).stream()
                .map(this::toDeliveryResponse).toList();
    }

    public List<DeliveryResponse> myDeliveries(String email) {
        User driver = currentUser(email);
        return deliveryRepository.findByDriver(driver).stream().map(this::toDeliveryResponse).toList();
    }

    public DeliveryResponse acceptDelivery(String email, Long deliveryId) {
        User driver = currentUser(email);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        if (delivery.getStatus() != DeliveryStatus.AVAILABLE || delivery.getDriver() != null) {
            throw new RuntimeException("Delivery is not available");
        }
        delivery.setDriver(driver);
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setAcceptedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
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
        return toDeliveryResponse(delivery);
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
                addr.getDetails()
        );
        String deliveryStatus = deliveryRepository.findByOrder(order)
                .map(d -> d.getStatus().name()).orElse(null);
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
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null
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
                delivery.getDeliveredAt() != null ? delivery.getDeliveredAt().toString() : null
        );
    }
}
