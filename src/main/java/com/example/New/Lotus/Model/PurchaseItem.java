package com.example.New.Lotus.Model;



import lombok.Data;

@Data
public class PurchaseItem {
    private String itemId;  // Links to Item collection
    private String name;    // Redundant but useful for invoices
    private String hsnCode;
    private int quantity;
    private double price;
    private double gstRate;
    private double total;       // price * quantity
    private double gstAmount;
}