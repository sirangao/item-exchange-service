package com.siran.itemExchange.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ListingInput {

    // FK to users.id — a plain id, never a nested Users object
    @NotNull
    private Integer userId;

    // FK to categories.id — nullable in the schema (ON DELETE SET NULL)
    private Integer categoryId;

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 65535)
    private String description;

    // Mirrors listings.listing_type ENUM('sell','exchange','both') exactly — anything
    // outside the database's enum passes validation here and then fails on insert.
    @NotBlank
    @Pattern(regexp = "sell|exchange|both",
            message = "must be one of: sell, exchange, both")
    private String listingType;

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)   // DECIMAL(10,2)
    private BigDecimal price;

    @NotBlank
    @Pattern(regexp = "new|like_new|good|fair|poor",
            message = "must be one of: new, like_new, good, fair, poor")
    private String conditionGrade;

    // Optional on create (defaults to 'available'); send it on update to change state.
    // Mirrors listings.status ENUM('available','pending','sold','exchanged').
    @Pattern(regexp = "available|pending|sold|exchanged",
            message = "must be one of: available, pending, sold, exchanged")
    private String status;

    @Size(max = 500)
    private String imageUrl;

    public Integer getUserId() {
        return userId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getListingType() {
        return listingType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getConditionGrade() {
        return conditionGrade;
    }

    public String getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}