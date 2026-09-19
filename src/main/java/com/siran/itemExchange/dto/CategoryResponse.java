package com.siran.itemExchange.dto;

import com.siran.itemExchange.dataObjects.Categories;
import lombok.Getter;

@Getter
public class CategoryResponse {

    private Integer id;
    private String name;

    public CategoryResponse(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CategoryResponse from(Categories c) {
        return new CategoryResponse(c.getId(), c.getName());
    }
}
