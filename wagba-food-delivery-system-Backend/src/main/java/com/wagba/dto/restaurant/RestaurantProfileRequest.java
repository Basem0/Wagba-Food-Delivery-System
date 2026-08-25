package com.wagba.dto.restaurant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RestaurantProfileRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(max = 120, message = "Restaurant name is too long")
    private String name;

    @Size(max = 1000, message = "Description is too long")
    private String description;

    private String imageUrl;

    @Size(max = 60, message = "Cuisine is too long")
    private String cuisine;

    @Min(value = 1, message = "Preparation time must be at least 1 minute")
    @Max(value = 240, message = "Preparation time cannot exceed 240 minutes")
    private Integer etaMinutes;

    @DecimalMin(value = "0.0", message = "Delivery fee cannot be negative")
    private BigDecimal deliveryFee;

    @DecimalMin(value = "0.0", message = "Minimum order cannot be negative")
    private BigDecimal minOrderTotal;

    @Size(max = 30, message = "Phone number is too long")
    private String phone;

    // Pickup address. Without these the driver has no way to reach the kitchen,
    // which is why they are captured at onboarding rather than later.
    private String city;
    private String street;
    private String buildingNumber;
    private String details;

    @DecimalMin(value = "-90.0", message = "Latitude is out of range")
    @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "Latitude is out of range")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude is out of range")
    @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "Longitude is out of range")
    private Double longitude;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
