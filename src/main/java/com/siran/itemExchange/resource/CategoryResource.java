package com.siran.itemExchange.resource;

import com.siran.itemExchange.dataRepositories.CategoriesRepository;
import com.siran.itemExchange.dto.CategoryResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Path("/categories")
public class CategoryResource {

    @Autowired
    CategoriesRepository categoriesRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCategories() {
        List<CategoryResponse> body = new ArrayList<>();
        categoriesRepository.findAll().forEach(c -> body.add(CategoryResponse.from(c)));
        body.sort(Comparator.comparing(CategoryResponse::getName));
        return Response.ok(body).build();
    }
}
