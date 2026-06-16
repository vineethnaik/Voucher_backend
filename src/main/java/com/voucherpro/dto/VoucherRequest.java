package com.voucherpro.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public class VoucherRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 200, message = "Course name must be at most 200 characters")
    private String title;

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Icon is required")
    private String iconName;

    @Positive(message = "Original price must be greater than zero")
    private double originalPrice;

    @Positive(message = "Discount price must be greater than zero")
    private double discountPrice;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotBlank(message = "Exam badge is required")
    @Size(max = 50, message = "Badge must be at most 50 characters")
    private String badge;

    @NotEmpty(message = "At least one prerequisite is required")
    private List<@NotBlank(message = "Prerequisite cannot be blank") String> requirements;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements;
    }
}
