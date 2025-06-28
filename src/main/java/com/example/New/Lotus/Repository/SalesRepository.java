package com.example.New.Lotus.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.New.Lotus.entity.PurchaseInvoice;
import com.example.New.Lotus.entity.SalesInvoice;

public interface SalesRepository extends MongoRepository<SalesInvoice, String> {
	@Query("{ 'invoiceDate': { $gte: ?0, $lt: ?1 } }")
	List<SalesInvoice> findInvoicesBetweenDates(LocalDate from, LocalDate toExclusive);
}
