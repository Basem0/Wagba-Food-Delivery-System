package com.wagba.dto.restaurant;

import java.math.BigDecimal;

public class RestaurantProfileRequest {

    private String name;
    private String description;
    private String imageUrl;
    private String cuisine;
    private Integer etaMinutes;
    private BigDecimal deliveryFee;
    private BigDecimal minOrderTotal;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public Integer getEtaMinutes() {
        return etaMinutes;
    }

    public void setEtaMinutes(Integer etaMinutes) {
        this.etaMinutes = etaMinutes;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getMinOrderTotal() {
        return minOrderTotal;
    }

    public void setMinOrderTotal(BigDecimal minOrderTotal) {
        this.minOrderTotal = minOrderTotal;
    }
}