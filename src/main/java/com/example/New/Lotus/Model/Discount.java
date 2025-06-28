package com.example.New.Lotus.Model;

import lombok.Data;

@Data
public class Discount {
    private String type;       // "percentage" or "amount"
    private double value;      // 10% or ₹100
}