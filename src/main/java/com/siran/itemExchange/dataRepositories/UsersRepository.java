package com.siran.itemExchange.dataRepositories;

import com.siran.itemExchange.dataObjects.Users;
import org.springframework.data.repository.CrudRepository;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface UsersRepository extends CrudRepository<Users, Integer> {

}