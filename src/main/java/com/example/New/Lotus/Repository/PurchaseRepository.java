package com.example.New.Lotus.Repository;

import java.util.*;
import java.time.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.New.Lotus.entity.PurchaseInvoice;

public interface PurchaseRepository extends MongoRepository<PurchaseInvoice, String> {
	@Query("{ 'invoiceDate': { $gte: ?0, $lt: ?1 } }")
	List<PurchaseInvoice> findInvoicesBetweenDates(LocalDate from, LocalDate toExclusive);

}