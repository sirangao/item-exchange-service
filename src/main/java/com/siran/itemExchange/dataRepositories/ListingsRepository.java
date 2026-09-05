package com.siran.itemExchange.dataRepositories;

import com.siran.itemExchange.dataObjects.Listings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface ListingsRepository extends JpaRepository<Listings, Integer> { //extends CrudRepository<Listings, Integer> { // or JpaRepository

}