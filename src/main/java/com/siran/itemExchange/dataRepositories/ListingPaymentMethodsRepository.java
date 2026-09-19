package com.siran.itemExchange.dataRepositories;

import com.siran.itemExchange.dataObjects.ListingPaymentMethods;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ListingPaymentMethodsRepository extends CrudRepository<ListingPaymentMethods, Integer> {

    List<ListingPaymentMethods> findByListingsId(Integer listingId);

    @Transactional
    void deleteByListingsId(Integer listingId);
}