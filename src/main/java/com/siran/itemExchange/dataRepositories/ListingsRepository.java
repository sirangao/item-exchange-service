package com.siran.itemExchange.dataRepositories;

import com.siran.itemExchange.dataObjects.Listings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// This will be AUTO IMPLEMENTED by Spring into a Bean called listingsRepository
// CRUD refers Create, Read, Update, Delete

public interface ListingsRepository extends JpaRepository<Listings, Integer> {

    // Listings.users and Listings.categories are LAZY, and Jersey requests run outside
    // Spring MVC's open-in-view, so the session is gone by the time a resource maps the
    // entity. Anything that reads the seller's username or the category name has to
    // fetch them up front. (getUsers().getId() happens to work without this — a lazy
    // proxy knows its own id — but getUsername() would throw.)
    String WITH_DETAILS = "select l from Listings l join fetch l.users u left join fetch l.categories c";

    @Query(WITH_DETAILS + " where l.id = :id")
    Optional<Listings> findByIdWithDetails(@Param("id") Integer id);

    @Query(WITH_DETAILS
            + " where l.status = :status"
            + " and (:listingType is null or l.listingType = :listingType)"
            + " and (:categoryName is null or c.name = :categoryName)"
            + " order by l.createdAt desc")
    List<Listings> browse(@Param("status") String status,
                          @Param("listingType") String listingType,
                          @Param("categoryName") String categoryName);

    @Query(WITH_DETAILS + " where u.id = :userId order by l.createdAt desc")
    List<Listings> findByUserIdWithDetails(@Param("userId") Integer userId);
}
