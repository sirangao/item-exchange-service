package com.siran.itemExchange.dataRepositories;

import com.siran.itemExchange.dataObjects.Users;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface UsersRepository extends CrudRepository<Users, Integer> { // or JpaRepository
    @Override
    Optional<Users> findById(Integer integer);

    // switch to custom queries
    Optional<Users> findByUsername(String username); // or: Users findByUsername(String username);
}