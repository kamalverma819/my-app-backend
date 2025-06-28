package com.example.New.Lotus.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "items")
@Data
public class Item {
 @Id
 private String id;
 private String name;
 private String hsnCode;
 private double buyingPrice;
 private double sellingPrice;
 private double gstRate;
 private int stock;
 private double commission = 0;          // Optional
 private boolean commissionPercent = false; // Optional
}