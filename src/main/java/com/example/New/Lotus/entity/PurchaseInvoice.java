package com.example.New.Lotus.entity;


import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.New.Lotus.Model.PurchaseItem;

import lombok.Data;

@Document(collection = "purchases")
@Data
public class PurchaseInvoice {
    @Id
    private String id;
    private LocalDate invoiceDate;
    private String invoiceNo;
    private String vendorName;
    private String gstin;
    private LocalDate date = LocalDate.now(); // Auto-set current date
    private List<PurchaseItem> items;
    private double subtotal;
    private double cgst;
    private double sgst;
    private double igst;
    private double freight;
    private double total;
}