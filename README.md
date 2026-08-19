# Wagba-Food-Delivery-System

## System Design

---

## 1. Functional Requirements

### Customer

- Customer should be able to register and login.
- Customer should be able to browse available restaurants.
- Customer should be able to view a restaurant's menu.
- Customer should be able to add food items to a cart.
- Customer should be able to update/remove items from the cart.
- Customer should be able to place an order.
- Customer should be able to provide/select a delivery address.
- Customer should be able to view their orders.
- Customer should be able to track the status of an active order.
- Customer should be able to cancel an order when allowed.
- Customer should be able to rate/review a completed order.

### Restaurant Owner

- Restaurant owner should be able to register.
- Restaurant owner should be able to create a restaurant.
- Restaurant owner should be able to update restaurant information.
- Restaurant owner should be able to manage food categories.
- Restaurant owner should be able to add/update/delete food items.
- Restaurant owner should be able to mark food items as available/unavailable.
- Restaurant owner should be able to view incoming orders.
- Restaurant owner should be able to accept/reject orders.
- Restaurant owner should be able to update the food preparation status.

### Driver

- Driver should be able to register.
- Driver should be able to become available/unavailable for deliveries.
- Driver should be able to view available delivery requests.
- Driver should be able to accept a delivery.
- Driver should be able to view delivery details.
- Driver should be able to mark an order as picked up.
- Driver should be able to mark an order as out for delivery.
- Driver should be able to mark an order as delivered.
- Driver should be able to view delivery history.

### Admin

- Admin should be able to login.
- Admin should be able to view/manage users.
- Admin should be able to activate/deactivate users.
- Admin should be able to review restaurant registration requests.
- Admin should be able to approve/reject restaurants.
- Admin should be able to review driver registration requests.
- Admin should be able to approve/reject drivers.
- Admin should be able to view all orders.
- Admin should be able to monitor the overall system.

---

## 2. Non-Functional Requirements

### 01- Performance

- Restaurant listing API should normally respond within 500ms under expected load.
- Menu retrieval should normally respond within 500ms.

### 02- Availability

- The system should remain available during normal operating hours.
- A failure in one operation should not corrupt existing orders.

### 03- Security

- Passwords must never be stored as plain text.
- Authenticated endpoints must require valid authentication.
- Users must only access resources they are authorized to access.
- Customer should not be able to access restaurant-owner/admin operations.

### 04- Data Integrity

- An order must preserve the price of food at the time the order was created.
- An order must not reference unavailable/nonexistent food.
- Order status transitions must follow valid business rules.
- Order creation should be transactional.

### 05- Scalability

Architecture must allow us to increase Number of :

- Customers
- Restaurants
- Orders
- Drivers

---

## 3- Core Entites

- User
- Restaurant
- Food
- Category
- Cart
- CartItem
- Order
- OrderItem
- Address
- Delivery
- Review

---

## 4. API / System Interface

### 1- Authentication

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
```

### 2- Restaurants

```http
GET    /api/v1/restaurants
GET    /api/v1/restaurants/{restaurantId}
POST   /api/v1/restaurants
PUT    /api/v1/restaurants/{restaurantId}
DELETE /api/v1/restaurants/{restaurantId}
```

### 3- Foods

```http
GET    /api/v1/restaurants/{restaurantId}/foods
POST   /api/v1/foods
GET    /api/v1/foods/{foodId}
PUT    /api/v1/foods/{foodId}
DELETE /api/v1/foods/{foodId}
```

### 4-Cart

```http
GET    /api/v1/cart
POST   /api/v1/cart/items
PUT    /api/v1/cart/items/{foodId}
DELETE /api/v1/cart/items/{foodId}
DELETE /api/v1/cart
```

### 5-Orders

```http
POST /api/v1/orders
GET  /api/v1/orders
GET  /api/v1/orders/{orderId}
POST /api/v1/orders/{orderId}/cancel
```

### 6- Restaurant Owner:

```http
GET  /api/v1/restaurant/orders
POST /api/v1/orders/{orderId}/accept
POST /api/v1/orders/{orderId}/reject
POST /api/v1/orders/{orderId}/preparing
POST /api/v1/orders/{orderId}/ready
```

### 7- Driver:

```http
GET  /api/v1/deliveries/available
POST /api/v1/deliveries/{deliveryId}/accept
POST /api/v1/deliveries/{deliveryId}/pickup
POST /api/v1/deliveries/{deliveryId}/out-for-delivery
POST /api/v1/deliveries/{deliveryId}/complete
```

### 8- Admin:

```http
GET   /api/v1/admin/users
PATCH /api/v1/admin/users/{userId}/status

GET   /api/v1/admin/restaurants
POST  /api/v1/admin/restaurants/{id}/approve
POST  /api/v1/admin/restaurants/{id}/reject

GET   /api/v1/admin/drivers
POST  /api/v1/admin/drivers/{id}/approve
POST  /api/v1/admin/drivers/{id}/reject

GET   /api/v1/admin/orders
```

---

## High Level Desgin

<img width="671" height="420" alt="image" src="https://github.com/user-attachments/assets/e0cf3e94-036d-4079-bf6b-bad9bf2eb509" />
