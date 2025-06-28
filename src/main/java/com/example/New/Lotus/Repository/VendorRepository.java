package com.example.New.Lotus.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.New.Lotus.entity.Vendor;

public interface VendorRepository extends MongoRepository<Vendor, String> {
    List<Vendor> findByNameContainingIgnoreCase(String name);
    Vendor findByGstin(String gstin);
}