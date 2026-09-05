package com.siran.itemExchange.dataRepositories;

import com.siran.itemExchange.dataObjects.Categories;
import org.springframework.data.repository.CrudRepository;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface CategoriesRepository extends CrudRepository<Categories, Integer> { // or JpaRepository

}