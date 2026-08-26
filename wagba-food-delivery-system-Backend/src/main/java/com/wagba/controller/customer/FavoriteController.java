package com.wagba.controller.customer;

import com.wagba.dto.favorite.FavoriteResponse;
import com.wagba.security.SecurityUtil;
import com.wagba.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@PreAuthorize("hasRole('CUSTOMER')")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> getFavorites() {
        return favoriteService.getFavorites(SecurityUtil.getCurrentUserEmail());
    }

    @PostMapping("/{restaurantId}")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long restaurantId) {
        boolean added = favoriteService.toggle(SecurityUtil.getCurrentUserEmail(), restaurantId);
        return ResponseEntity.ok(Map.of("favorited", added));
    }
}
