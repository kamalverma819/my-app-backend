package com.example.New.Lotus.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.New.Lotus.Repository.*;
import com.example.New.Lotus.Model.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private PurchaseRepository purchaseRepo;
    @Autowired private SalesRepository salesRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private VendorRepository vendorRepo;
    @Autowired private ItemRepository itemRepo;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalPurchases(purchaseRepo.count());
        stats.setTotalSales(salesRepo.count());
        stats.setTotalCustomers(customerRepo.count());
        stats.setTotalVendors(vendorRepo.count());
        stats.setTotalItems(itemRepo.count());

        return ResponseEntity.ok(stats);
    }
}
