package com.wagba.service;

import com.wagba.entity.Category;
import com.wagba.entity.Food;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.entity.enums.OnboardingStatus;
import com.wagba.entity.enums.RestaurantStatus;
import com.wagba.entity.enums.UserRole;
import com.wagba.entity.enums.UserStatus;
import com.wagba.repository.CartItemRepository;
import com.wagba.repository.CartRepository;
import com.wagba.repository.CategoryRepository;
import com.wagba.repository.DeliveryRepository;
import com.wagba.repository.FoodRepository;
import com.wagba.repository.OrderItemRepository;
import com.wagba.repository.OrderRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class RestaurantSeedService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final FoodRepository foodRepository;
        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final DeliveryRepository deliveryRepository;
        private final CartRepository cartRepository;
        private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RestaurantSeedService(
            RestaurantRepository restaurantRepository,
            CategoryRepository categoryRepository,
            FoodRepository foodRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        DeliveryRepository deliveryRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.foodRepository = foodRepository;
                this.orderRepository = orderRepository;
                this.orderItemRepository = orderItemRepository;
                this.deliveryRepository = deliveryRepository;
                this.cartRepository = cartRepository;
                this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void seedRestaurants() {
        clearRestaurantData();
        System.out.println("Refreshing restaurant catalog with 12 seeded restaurants.");

        List<RestaurantSpec> specs = Arrays.asList(
                new RestaurantSpec(
                        "Bazooka",
                        "Fast casual burgers and wraps",
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0b/Bazooka_Logo.svg/512px-Bazooka_Logo.svg.png",
                        "Burgers",
                        25,
                        new BigDecimal("15.00"),
                        new BigDecimal("60.00"),
                        4.7,
                        "01000000001",
                        "Cairo",
                        "Al-Maadi",
                        "12",
                        "A",
                        "Popular local favorite",
                        29.9737,
                        31.2357,
                        "bazooka.owner@wagba.com",
                        List.of(
                                new CategorySpec("Signature Burgers", List.of(
                                        new FoodSpec("Bazooka Burger", "Juicy grilled burger with cheddar and smoky sauce", new BigDecimal("129.00"), new BigDecimal("109.00"), "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Crispy Chicken Burger", "Crispy chicken, lettuce and garlic mayo", new BigDecimal("119.00"), new BigDecimal("99.00"), "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Loaded Fries", "Fries with cheese sauce and jalapenos", new BigDecimal("69.00"), new BigDecimal("59.00"), "https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Wraps & Combos", List.of(
                                        new FoodSpec("Chicken Wrap", "Grilled chicken wrap with fries", new BigDecimal("99.00"), new BigDecimal("89.00"), "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Family Combo", "2 burgers, 2 fries and 2 soft drinks", new BigDecimal("249.00"), new BigDecimal("229.00"), "https://images.unsplash.com/photo-1550317138-10000687a72b?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Coca Cola", "Chilled soft drink", new BigDecimal("22.00"), null, "https://images.unsplash.com/photo-1622483767028-3f66f2b2a1d3?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "كشري التحرير",
                        "Authentic Egyptian koshary and comfort food",
                        "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80",
                        "Egyptian",
                        30,
                        new BigDecimal("12.00"),
                        new BigDecimal("35.00"),
                        4.8,
                        "01000000002",
                        "Cairo",
                        "Downtown",
                        "87",
                        "B",
                        "Classic koshary and grilled meals",
                        30.0444,
                        31.2357,
                        "koshary.owner@wagba.com",
                        List.of(
                                new CategorySpec("Koshary", List.of(
                                        new FoodSpec("Classic Koshary", "Rice, lentils, macaroni and tomato sauce", new BigDecimal("55.00"), new BigDecimal("48.00"), "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Koshary Deluxe", "With crispy onions, fried chickpeas and extra sauce", new BigDecimal("80.00"), new BigDecimal("70.00"), "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Koshary with Chicken", "Grilled chicken topping for a fuller meal", new BigDecimal("95.00"), new BigDecimal("85.00"), "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Hot Meals", List.of(
                                        new FoodSpec("Grilled Chicken Plate", "Served with rice and salad", new BigDecimal("120.00"), new BigDecimal("110.00"), "https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Falafel Plate", "Fresh falafel, salad and tahini", new BigDecimal("70.00"), new BigDecimal("60.00"), "https://images.unsplash.com/photo-1615937691194-97dbd3f3dc29?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Fresh Lemonade", "Cold and refreshing", new BigDecimal("25.00"), null, "https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "باب جونز",
                        "Grill, burgers and family meals",
                        "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=800&q=80",
                        "Burgers",
                        27,
                        new BigDecimal("14.00"),
                        new BigDecimal("55.00"),
                        4.6,
                        "01000000003",
                        "Cairo",
                        "Nasr City",
                        "55",
                        "C",
                        "Comfort food for every craving",
                        30.0500,
                        31.3500,
                        "papajones.owner@wagba.com",
                        List.of(
                                new CategorySpec("Burgers", List.of(
                                        new FoodSpec("Classic Burger", "Beef patty, cheese and pickles", new BigDecimal("110.00"), new BigDecimal("95.00"), "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Double Patty Burger", "Double beef with cheddar", new BigDecimal("145.00"), new BigDecimal("125.00"), "https://images.unsplash.com/photo-1550317138-10000687a72b?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Crispy Chicken Burger", "With spicy mayo", new BigDecimal("120.00"), new BigDecimal("105.00"), "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Sides & Drinks", List.of(
                                        new FoodSpec("French Fries", "Golden crispy fries", new BigDecimal("33.00"), new BigDecimal("25.00"), "https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Onion Rings", "Crunchy and savory", new BigDecimal("39.00"), null, "https://images.unsplash.com/photo-1626700051175-6818013e1d4f?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Cold Brew", "Smooth and chilled", new BigDecimal("35.00"), null, "https://images.unsplash.com/photo-1498804103079-a4f4f7de8b3b?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "بيتزا هات",
                        "Wood-fired pizza and creamy pasta",
                        "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=800&q=80",
                        "Pizza",
                        22,
                        new BigDecimal("10.00"),
                        new BigDecimal("50.00"),
                        4.7,
                        "01000000004",
                        "Cairo",
                        "Mohandessin",
                        "30",
                        "D",
                        "Fresh pizza with rich toppings",
                        30.0359,
                        31.2004,
                        "pizzahut.owner@wagba.com",
                        List.of(
                                new CategorySpec("Pizza", List.of(
                                        new FoodSpec("Margherita Pizza", "Fresh mozzarella and basil", new BigDecimal("155.00"), new BigDecimal("145.00"), "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Pepperoni Pizza", "Classic pepperoni and cheese", new BigDecimal("175.00"), new BigDecimal("165.00"), "https://images.unsplash.com/photo-1548365328-9f547fb9587c?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Supreme Pizza", "Loaded with veggies and meat", new BigDecimal("190.00"), new BigDecimal("175.00"), "https://images.unsplash.com/photo-1514989940723-e8e51635b782?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Pasta & Sides", List.of(
                                        new FoodSpec("Creamy Alfredo Pasta", "Rich and smooth classic sauce", new BigDecimal("130.00"), new BigDecimal("120.00"), "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Garlic Bread", "Baked with herb butter", new BigDecimal("45.00"), null, "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Soft Drink", "Refreshing choice for the meal", new BigDecimal("22.00"), null, "https://images.unsplash.com/photo-1622483767028-3f66f2b2a1d3?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "KFC",
                        "Crispy chicken and crunchy sides",
                        "https://upload.wikimedia.org/wikipedia/en/thumb/5/5d/KFC_logo.svg/256px-KFC_logo.svg.png",
                        "Chicken",
                        26,
                        new BigDecimal("15.00"),
                        new BigDecimal("60.00"),
                        4.5,
                        "01000000005",
                        "Cairo",
                        "Dokki",
                        "42",
                        "E",
                        "Original recipe chicken and family buckets",
                        30.0454,
                        31.2056,
                        "kfc.owner@wagba.com",
                        List.of(
                                new CategorySpec("Buckets", List.of(
                                        new FoodSpec("Hot & Crispy Bucket", "12 pieces of signature chicken", new BigDecimal("290.00"), new BigDecimal("260.00"), "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Family Feast", "Chicken, fries and coleslaw", new BigDecimal("340.00"), new BigDecimal("310.00"), "https://images.unsplash.com/photo-1562967916-eb1b7f8ac0b7?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Zinger Burger", "Spicy chicken burger", new BigDecimal("120.00"), new BigDecimal("105.00"), "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Sides", List.of(
                                        new FoodSpec("French Fries", "Classic crispy fries", new BigDecimal("45.00"), new BigDecimal("38.00"), "https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Cheese Fries", "Loaded with cheese sauce", new BigDecimal("55.00"), new BigDecimal("49.00"), "https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Coleslaw", "Fresh creamy slaw", new BigDecimal("30.00"), null, "https://images.unsplash.com/photo-1546793665-c74683f339c1?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "ستاربكس",
                        "Coffee, cold brews and bakery bites",
                        "https://upload.wikimedia.org/wikipedia/en/thumb/d/d3/Starbucks_Corporation_Logo_2011.svg/256px-Starbucks_Corporation_Logo_2011.svg.png",
                        "Coffee",
                        18,
                        new BigDecimal("9.00"),
                        new BigDecimal("40.00"),
                        4.9,
                        "01000000006",
                        "Cairo",
                        "Maadi",
                        "65",
                        "A",
                        "Premium coffee and dessert bar",
                        29.9608,
                        31.2536,
                        "starbucks.owner@wagba.com",
                        List.of(
                                new CategorySpec("Coffee", List.of(
                                        new FoodSpec("Caramel Latte", "Espresso with caramel and milk", new BigDecimal("62.00"), new BigDecimal("58.00"), "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Cold Brew", "Smooth and chilled coffee", new BigDecimal("58.00"), new BigDecimal("52.00"), "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Vanilla Frappuccino", "Creamy and sweet blended coffee", new BigDecimal("75.00"), new BigDecimal("68.00"), "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Bakery", List.of(
                                        new FoodSpec("Cheese Danish", "Buttery pastry", new BigDecimal("42.00"), null, "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Blueberry Muffin", "Soft and fruity bakery bite", new BigDecimal("39.00"), null, "https://images.unsplash.com/photo-1607948996333-40d0dd0d5d35?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Iced Tea", "Refreshing hibiscus tea", new BigDecimal("30.00"), null, "https://images.unsplash.com/photo-1513558161293-cdaf765ed2e6?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "Gad",
                        "Grilled favorites and home-style meals",
                        "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=800&q=80",
                        "Grill",
                        24,
                        new BigDecimal("11.00"),
                        new BigDecimal("45.00"),
                        4.6,
                        "01000000007",
                        "Cairo",
                        "Zamalek",
                        "20",
                        "F",
                        "Fresh grilled meals and wraps",
                        30.0607,
                        31.2121,
                        "gad.owner@wagba.com",
                        List.of(
                                new CategorySpec("Grill", List.of(
                                        new FoodSpec("Chicken Shawarma Plate", "Chicken shawarma with fries and salad", new BigDecimal("110.00"), new BigDecimal("95.00"), "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Beef Kofta Plate", "Fresh grilled kofta and rice", new BigDecimal("130.00"), new BigDecimal("118.00"), "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Mix Grill", "Selection of grilled proteins", new BigDecimal("190.00"), new BigDecimal("170.00"), "https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Wraps", List.of(
                                        new FoodSpec("Chicken Wrap", "Loaded with pickles and sauce", new BigDecimal("75.00"), new BigDecimal("65.00"), "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Falafel Wrap", "Crispy falafel with hummus", new BigDecimal("60.00"), new BigDecimal("52.00"), "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Fresh Mint Juice", "Cold and refreshing", new BigDecimal("25.00"), null, "https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "McDonald's",
                        "World-famous burgers and breakfast",
                        "https://upload.wikimedia.org/wikipedia/commons/3/36/McDonald%27s_Golden_Arches.svg",
                        "Fast Food",
                        20,
                        new BigDecimal("8.00"),
                        new BigDecimal("50.00"),
                        4.4,
                        "01000000008",
                        "Cairo",
                        "Heliopolis",
                        "18",
                        "G",
                        "Fast service and classic meals",
                        30.0962,
                        31.3289,
                        "mcdonalds.owner@wagba.com",
                        List.of(
                                new CategorySpec("Signature Meals", List.of(
                                        new FoodSpec("Big Mac", "Double beef patties and special sauce", new BigDecimal("105.00"), new BigDecimal("95.00"), "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Chicken McNuggets", "Crispy golden bites", new BigDecimal("86.00"), new BigDecimal("79.00"), "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Quarter Pounder", "Juicy beef burger", new BigDecimal("112.00"), new BigDecimal("98.00"), "https://images.unsplash.com/photo-1550317138-10000687a72b?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Sides & Drinks", List.of(
                                        new FoodSpec("Large Fries", "Hot and salted", new BigDecimal("35.00"), new BigDecimal("28.00"), "https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Apple Pie", "Warm pastry dessert", new BigDecimal("30.00"), null, "https://images.unsplash.com/photo-1517433670267-08bbd4be890f?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Coke Bottle", "Ice-cold soft drink", new BigDecimal("22.00"), null, "https://images.unsplash.com/photo-1622483767028-3f66f2b2a1d3?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "Burger King",
                        "Flame-grilled burgers and premium combos",
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/8/85/Burger_King_logo_%281999%29.svg/512px-Burger_King_logo_%281999%29.svg.png",
                        "Burgers",
                        21,
                        new BigDecimal("10.00"),
                        new BigDecimal("55.00"),
                        4.5,
                        "01000000009",
                        "Cairo",
                        "New Cairo",
                        "70",
                        "H",
                        "Flame-grilled classics and tasty sides",
                        30.0286,
                        31.4517,
                        "burgerking.owner@wagba.com",
                        List.of(
                                new CategorySpec("Burgers", List.of(
                                        new FoodSpec("Whopper", "Flame-grilled beef burger", new BigDecimal("120.00"), new BigDecimal("100.00"), "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Chicken Royale", "Crispy chicken burger", new BigDecimal("110.00"), new BigDecimal("95.00"), "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Double Whopper", "Extra-large burger for big cravings", new BigDecimal("165.00"), new BigDecimal("145.00"), "https://images.unsplash.com/photo-1550317138-10000687a72b?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Sides", List.of(
                                        new FoodSpec("Onion Rings", "Golden crunchy rings", new BigDecimal("42.00"), new BigDecimal("35.00"), "https://images.unsplash.com/photo-1626700051175-6818013e1d4f?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Cheese Sticks", "Crispy and cheesy", new BigDecimal("46.00"), null, "https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Sprite", "Fresh lemon-lime drink", new BigDecimal("22.00"), null, "https://images.unsplash.com/photo-1622483767028-3f66f2b2a1d3?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "Domino's Pizza",
                        "Pizza delivered fresh and hot",
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Domino%27s_pizza_logo.svg/512px-Domino%27s_pizza_logo.svg.png",
                        "Pizza",
                        23,
                        new BigDecimal("12.00"),
                        new BigDecimal("60.00"),
                        4.7,
                        "01000000010",
                        "Cairo",
                        "6th October",
                        "15",
                        "I",
                        "Handcrafted pizzas and dips",
                        29.9726,
                        30.9602,
                        "dominos.owner@wagba.com",
                        List.of(
                                new CategorySpec("Pizza", List.of(
                                        new FoodSpec("Pepperoni Pizza", "Classic cheese and pepperoni", new BigDecimal("180.00"), new BigDecimal("165.00"), "https://images.unsplash.com/photo-1548365328-9f547fb9587c?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Cheese Pizza", "Rich mozzarella and herb crust", new BigDecimal("150.00"), new BigDecimal("135.00"), "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Veggie Supreme", "Bell peppers, onion and olives", new BigDecimal("170.00"), new BigDecimal("155.00"), "https://images.unsplash.com/photo-1514989940723-e8e51635b782?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Extras", List.of(
                                        new FoodSpec("Garlic Bread", "Golden and buttery", new BigDecimal("45.00"), null, "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Buffalo Wings", "Crispy and spicy", new BigDecimal("90.00"), new BigDecimal("80.00"), "https://images.unsplash.com/photo-1527477396000-e27163b481c2?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Coke Zero", "Zero-sugar soft drink", new BigDecimal("22.00"), null, "https://images.unsplash.com/photo-1622483767028-3f66f2b2a1d3?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "Shawarma House",
                        "Fresh shawarma and street food classics",
                        "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?auto=format&fit=crop&w=800&q=80",
                        "Middle Eastern",
                        18,
                        new BigDecimal("9.00"),
                        new BigDecimal("40.00"),
                        4.6,
                        "01000000011",
                        "Cairo",
                        "Mokattam",
                        "8",
                        "J",
                        "Fast authentic shawarma and wraps",
                        30.1290,
                        31.2660,
                        "shawarma.owner@wagba.com",
                        List.of(
                                new CategorySpec("Wraps", List.of(
                                        new FoodSpec("Chicken Shawarma Wrap", "Tender chicken, pickles and garlic sauce", new BigDecimal("55.00"), new BigDecimal("50.00"), "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Cheese Shawarma", "Creamy cheese and grilled chicken", new BigDecimal("68.00"), new BigDecimal("62.00"), "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Falafel Wrap", "Crunchy falafel with tahini", new BigDecimal("50.00"), new BigDecimal("45.00"), "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Plates", List.of(
                                        new FoodSpec("Chicken Plate", "Served with rice and salad", new BigDecimal("90.00"), new BigDecimal("82.00"), "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("French Fries", "Extra crispy", new BigDecimal("30.00"), null, "https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Mango Juice", "Fresh and tropical", new BigDecimal("25.00"), null, "https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                ),
                new RestaurantSpec(
                        "Pizza Hut",
                        "Pan pizzas, pasta and family deals",
                        "https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Pizza_Hut_logo.svg/512px-Pizza_Hut_logo.svg.png",
                        "Pizza",
                        28,
                        new BigDecimal("14.00"),
                        new BigDecimal("60.00"),
                        4.6,
                        "01000000012",
                        "Cairo",
                        "Ramses",
                        "26",
                        "K",
                        "Value meals and warm pan pizzas",
                        30.0235,
                        31.2447,
                        "pizzahut2.owner@wagba.com",
                        List.of(
                                new CategorySpec("Pan Pizza", List.of(
                                        new FoodSpec("Supreme Pan Pizza", "Loaded toppings and rich cheese", new BigDecimal("195.00"), new BigDecimal("175.00"), "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Meat Lovers", "Pepperoni, beef and chicken", new BigDecimal("210.00"), new BigDecimal("190.00"), "https://images.unsplash.com/photo-1548365328-9f547fb9587c?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Veggie Delight", "Mushrooms, olives and peppers", new BigDecimal("180.00"), new BigDecimal("165.00"), "https://images.unsplash.com/photo-1514989940723-e8e51635b782?auto=format&fit=crop&w=900&q=80")
                                )),
                                new CategorySpec("Pasta & Drinks", List.of(
                                        new FoodSpec("Chicken Alfredo", "Rich creamy pasta", new BigDecimal("140.00"), new BigDecimal("125.00"), "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Garlic Knots", "Soft and savory", new BigDecimal("38.00"), null, "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=900&q=80"),
                                        new FoodSpec("Pepsi", "Cold drink with meal", new BigDecimal("22.00"), null, "https://images.unsplash.com/photo-1622483767028-3f66f2b2a1d3?auto=format&fit=crop&w=900&q=80")
                                ))
                        )
                )
        );

        for (RestaurantSpec spec : specs) {
            User owner = upsertOwner(spec.ownerEmail, spec.restaurantName);

            Restaurant restaurant = new Restaurant();
            restaurant.setName(spec.restaurantName);
            restaurant.setDescription(spec.description);
            restaurant.setImageUrl(spec.logoUrl);
            restaurant.setCuisine(spec.cuisine);
            restaurant.setEtaMinutes(spec.etaMinutes);
            restaurant.setDeliveryFee(spec.deliveryFee);
            restaurant.setMinOrderTotal(spec.minOrderTotal);
            restaurant.setAvgRating(spec.avgRating);
            restaurant.setPhone(spec.phone);
            restaurant.setCity(spec.city);
            restaurant.setStreet(spec.street);
            restaurant.setBuildingNumber(spec.buildingNumber);
            restaurant.setApartment(spec.apartment);
            restaurant.setDetails(spec.details);
            restaurant.setLatitude(spec.latitude);
            restaurant.setLongitude(spec.longitude);
            restaurant.setStatus(RestaurantStatus.APPROVED);
            restaurant.setOwner(owner);

            Restaurant savedRestaurant = restaurantRepository.save(restaurant);

            for (CategorySpec categorySpec : spec.categories) {
                Category category = new Category();
                category.setName(categorySpec.name);
                category.setRestaurant(savedRestaurant);
                Category savedCategory = categoryRepository.save(category);

                for (FoodSpec foodSpec : categorySpec.items) {
                    Food food = new Food();
                    food.setName(foodSpec.name);
                    food.setDescription(foodSpec.description);
                    food.setPrice(foodSpec.price);
                    food.setDiscountPrice(foodSpec.discountPrice);
                    food.setImageUrl(foodSpec.imageUrl);
                    food.setAvailable(true);
                    food.setCategory(savedCategory);
                    foodRepository.save(food);
                }
            }
        }

        System.out.println("Seeded 12 restaurant records with menu data.");
    }

    private void clearRestaurantData() {
                cartItemRepository.deleteAll();
                cartRepository.deleteAll();
                deliveryRepository.deleteAll();
                orderItemRepository.deleteAll();
                orderRepository.deleteAll();
        foodRepository.deleteAll();
        categoryRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    private User upsertOwner(String email, String restaurantName) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User owner = new User();
                    owner.setName(restaurantName + " Owner");
                    owner.setEmail(email);
                    owner.setPassword(passwordEncoder.encode("123456"));
                    owner.setRole(UserRole.RESTAURANT_OWNER);
                    owner.setStatus(UserStatus.ACTIVE);
                    owner.setEmailVerified(true);
                    owner.setOnboardingStatus(OnboardingStatus.COMPLETED);
                    return userRepository.save(owner);
                });
    }

    private static class RestaurantSpec {
        private final String restaurantName;
        private final String description;
        private final String logoUrl;
        private final String cuisine;
        private final Integer etaMinutes;
        private final BigDecimal deliveryFee;
        private final BigDecimal minOrderTotal;
        private final Double avgRating;
        private final String phone;
        private final String city;
        private final String street;
        private final String buildingNumber;
        private final String apartment;
        private final String details;
        private final Double latitude;
        private final Double longitude;
        private final String ownerEmail;
        private final List<CategorySpec> categories;

        private RestaurantSpec(
                String restaurantName,
                String description,
                String logoUrl,
                String cuisine,
                Integer etaMinutes,
                BigDecimal deliveryFee,
                BigDecimal minOrderTotal,
                Double avgRating,
                String phone,
                String city,
                String street,
                String buildingNumber,
                String apartment,
                String details,
                Double latitude,
                Double longitude,
                String ownerEmail,
                List<CategorySpec> categories
        ) {
            this.restaurantName = restaurantName;
            this.description = description;
            this.logoUrl = logoUrl;
            this.cuisine = cuisine;
            this.etaMinutes = etaMinutes;
            this.deliveryFee = deliveryFee;
            this.minOrderTotal = minOrderTotal;
            this.avgRating = avgRating;
            this.phone = phone;
            this.city = city;
            this.street = street;
            this.buildingNumber = buildingNumber;
            this.apartment = apartment;
            this.details = details;
            this.latitude = latitude;
            this.longitude = longitude;
            this.ownerEmail = ownerEmail;
            this.categories = categories;
        }
    }

    private static class CategorySpec {
        private final String name;
        private final List<FoodSpec> items;

        private CategorySpec(String name, List<FoodSpec> items) {
            this.name = name;
            this.items = items;
        }
    }

    private static class FoodSpec {
        private final String name;
        private final String description;
        private final BigDecimal price;
        private final BigDecimal discountPrice;
        private final String imageUrl;

        private FoodSpec(
                String name,
                String description,
                BigDecimal price,
                BigDecimal discountPrice,
                String imageUrl
        ) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.discountPrice = discountPrice;
            this.imageUrl = imageUrl;
        }
    }
}
