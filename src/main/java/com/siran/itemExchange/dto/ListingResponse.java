package com.siran.itemExchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.siran.itemExchange.dataObjects.Categories;
import com.siran.itemExchange.dataObjects.Listings;
import com.siran.itemExchange.dataObjects.Users;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
public class ListingResponse {

    private Integer id;
    private Integer userId;
    private Integer categoryId;
    private String title;
    private String description;
    private String listingType;
    private BigDecimal price;
    private String conditionGrade;
    private String status;
    private String imageUrl;

    // Denormalised for the client: a listing card shows the seller and category by name,
    // and the detail page shows the seller's contact details. Without these the frontend
    // would need a second request per card.
    private String categoryName;
    private String sellerUsername;
    private String sellerEmail;
    private String sellerPhone;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date updatedAt;

    public ListingResponse(Integer id, Integer userId, Integer categoryId, String title, String description,
                           String listingType, BigDecimal price, String conditionGrade, String status,
                           String imageUrl, String categoryName, String sellerUsername, String sellerEmail,
                           String sellerPhone, Date createdAt, Date updatedAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.listingType = listingType;
        this.price = price;
        this.conditionGrade = conditionGrade;
        this.status = status;
        this.imageUrl = imageUrl;
        this.categoryName = categoryName;
        this.sellerUsername = sellerUsername;
        this.sellerEmail = sellerEmail;
        this.sellerPhone = sellerPhone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * The listing must have been loaded with its seller and category already fetched —
     * see ListingsRepository.WITH_DETAILS — or reading the seller's username throws.
     */
    public static ListingResponse from(Listings l) {
        Categories c = l.getCategories();
        Users seller = l.getUsers();
        return new ListingResponse(
                l.getId(),
                seller.getId(),
                c == null ? null : c.getId(),
                l.getTitle(),
                l.getDescription(),
                l.getListingType(),
                l.getPrice(),
                l.getConditionGrade(),
                l.getStatus(),
                l.getImageUrl(),
                c == null ? null : c.getName(),
                seller.getUsername(),
                seller.getEmail(),
                seller.getPhone(),
                l.getCreatedAt(),
                l.getUpdatedAt());
    }
}
