// com/example/New/Lotus/Repository/CustomerRepository.java
package com.example.New.Lotus.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.New.Lotus.entity.Customer;

public interface CustomerRepository extends MongoRepository<Customer, String> {
    boolean existsByGstin(String gstin);
    boolean existsByNameAndGstin(String name, String gstin);
}
