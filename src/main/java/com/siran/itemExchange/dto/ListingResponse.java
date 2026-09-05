package com.siran.itemExchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.siran.itemExchange.dataObjects.Categories;
import com.siran.itemExchange.dataObjects.Listings;
import lombok.AllArgsConstructor;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date updatedAt;

    public ListingResponse(Integer id, Integer userId, Integer categoryId, String title, String description, String listingType, BigDecimal price, String conditionGrade, String status, String imageUrl, Date createdAt, Date updatedAt) {
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ListingResponse from(Listings l) {
        Categories c = l.getCategories();
        return new ListingResponse(
                l.getId(),
                l.getUsers().getId(),
                c == null ? null : c.getId(),
                l.getTitle(),
                l.getDescription(),
                l.getListingType(),
                l.getPrice(),
                l.getConditionGrade(),
                l.getStatus(),
                l.getImageUrl(),
                l.getCreatedAt(),
                l.getUpdatedAt());
    }
}