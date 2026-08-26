package com.wagba.service;

import com.wagba.dto.favorite.FavoriteResponse;
import com.wagba.entity.Favorite;
import com.wagba.entity.Restaurant;
import com.wagba.entity.User;
import com.wagba.repository.FavoriteRepository;
import com.wagba.repository.RestaurantRepository;
import com.wagba.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           RestaurantRepository restaurantRepository,
                           UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<FavoriteResponse> getFavorites(String email) {
        User user = currentUser(email);
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(f -> {
                    Restaurant r = f.getRestaurant();
                    return new FavoriteResponse(
                            f.getId(),
                            r.getId(),
                            r.getName(),
                            r.getCuisine(),
                            r.getImageUrl(),
                            r.getAvgRating() != null ? java.math.BigDecimal.valueOf(r.getAvgRating()) : null,
                            f.getCreatedAt() != null ? f.getCreatedAt().toString() : null
                    );
                }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public boolean toggle(String email, Long restaurantId) {
        User user = currentUser(email);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        var existing = favoriteRepository.findByUserAndRestaurant(user, restaurant);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        } else {
            Favorite fav = new Favorite();
            fav.setUser(user);
            fav.setRestaurant(restaurant);
            favoriteRepository.save(fav);
            return true;
        }
    }
}
