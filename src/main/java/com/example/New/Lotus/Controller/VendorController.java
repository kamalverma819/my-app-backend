package com.example.New.Lotus.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.New.Lotus.Repository.VendorRepository;
import com.example.New.Lotus.entity.Vendor;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {
    
    @Autowired
    private VendorRepository vendorRepo;

    // Create new vendor
    @PostMapping
    public ResponseEntity<Vendor> createVendor(@RequestBody Vendor vendor) {
        Vendor savedVendor = vendorRepo.save(vendor);
        return ResponseEntity.ok(savedVendor);
    }

    // Get all vendors
    @GetMapping
    public ResponseEntity<List<Vendor>> getAllVendors() {
        return ResponseEntity.ok(vendorRepo.findAll());
    }

    // Get vendor by ID
    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(@PathVariable String id) {
        Optional<Vendor> vendor = vendorRepo.findById(id);
        return vendor.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    // Update vendor
    @PutMapping("/{id}")
    public ResponseEntity<Vendor> updateVendor(
        @PathVariable String id, 
        @RequestBody Vendor vendorDetails
    ) {
        Optional<Vendor> vendorOptional = vendorRepo.findById(id);
        
        if (vendorOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Vendor vendor = vendorOptional.get();
        vendor.setName(vendorDetails.getName());
        vendor.setGstin(vendorDetails.getGstin());
        vendor.setContact(vendorDetails.getContact());
        vendor.setAddress(vendorDetails.getAddress());
        
        Vendor updatedVendor = vendorRepo.save(vendor);
        return ResponseEntity.ok(updatedVendor);
    }

    // Search vendors by name
    @GetMapping("/search")
    public ResponseEntity<List<Vendor>> searchVendors(@RequestParam String name) {
        return ResponseEntity.ok(vendorRepo.findByNameContainingIgnoreCase(name));
    }

    // Delete vendor
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable String id) {
        vendorRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}