// com/example/New/Lotus/Controller/CustomerController.java
package com.example.New.Lotus.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.New.Lotus.Repository.CustomerRepository;
import com.example.New.Lotus.entity.Customer;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepo;

    @PostMapping
    public ResponseEntity<?> addCustomer(@RequestBody Customer customer) {
        if (customerRepo.existsByNameAndGstin(customer.getName(), customer.getGstin())) {
            return ResponseEntity.badRequest().body("Customer with same name and GSTIN already exists.");
        }
        return ResponseEntity.ok(customerRepo.save(customer));
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String id) {
        return customerRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable String id, @RequestBody Customer updatedCustomer) {
        return customerRepo.findById(id)
                .map(existing -> {
                    existing.setName(updatedCustomer.getName());
                    existing.setContact(updatedCustomer.getContact());
                    existing.setAddress(updatedCustomer.getAddress());
                    existing.setGstin(updatedCustomer.getGstin());
                    return ResponseEntity.ok(customerRepo.save(existing));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable String id) {
        if (!customerRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        customerRepo.deleteById(id);
        return ResponseEntity.ok("Customer deleted successfully");
    }
}
