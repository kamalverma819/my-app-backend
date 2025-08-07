package com.example.New.Lotus.Model;



import lombok.Data;

@Data
public class SalesItem {
    private String itemId;       // Reference to Item collection
    private String name;         // Redundant but useful for reports
    private String hsnCode;
    private int quantity;
    private double price;       // Selling price
    private double gstRate;
    private double total;       // price * quantity
    private double gstAmount;   // (price * quantity * gstRate)/100
    private double discount;
    private int originalQuantity;
    private int currentStock;

}