package com.example.New.Lotus.Controller;

import java.util.*;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.New.Lotus.Model.Item;
import com.example.New.Lotus.Repository.ItemRepository;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepo;

    // Get all items
    @GetMapping
    public List<Item> getAllItems() {
        return itemRepo.findAll();
    }

    // Get item by ID
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable String id) {
        Optional<Item> item = itemRepo.findById(id);
        return item.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Add a new item
    @PostMapping
    public ResponseEntity<?> addItem(@RequestBody Item item) {
        Optional<Item> existingItem = itemRepo.findByNameAndHsnCode(item.getName(), item.getHsnCode());

        if (existingItem.isPresent()) {
            return ResponseEntity.badRequest().body("Item with the same name and HSN already exists.");
        }

        Item savedItem = itemRepo.save(item);
        return ResponseEntity.ok(savedItem);
    }


    // Update an item
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable String id, @RequestBody Item updatedData) {
        return itemRepo.findById(id).map(existingItem -> {
            // Update specific fields only
            existingItem.setBuyingPrice(updatedData.getBuyingPrice());
            existingItem.setStock(existingItem.getStock() + updatedData.getStock());

            // Optional: update GST rate or HSN if needed
            existingItem.setGstRate(updatedData.getGstRate());
            existingItem.setHsnCode(updatedData.getHsnCode());

            Item saved = itemRepo.save(existingItem);
            return ResponseEntity.ok(saved);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }


    // Delete an item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        if (itemRepo.existsById(id)) {
            itemRepo.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/commission")
    public ResponseEntity<?> updateItemCommission(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload) {

        Optional<Item> optional = itemRepo.findById(id);
        if (optional.isPresent()) {
            Item item = optional.get();
            item.setCommission(Double.parseDouble(payload.get("commission").toString()));
            item.setCommissionPercent(Boolean.parseBoolean(payload.get("isPercent").toString()));
            itemRepo.save(item);
            return ResponseEntity.ok("Commission updated for item");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found");
    }

}