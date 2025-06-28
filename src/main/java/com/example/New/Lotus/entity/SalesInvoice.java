package com.example.New.Lotus.entity;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.New.Lotus.Model.Discount;
import com.example.New.Lotus.Model.SalesItem;

import lombok.Data;

@Document(collection = "sales")
@Data
public class SalesInvoice {

    @Id
    private String id;

    private LocalDate invoiceDate;         // e.g. "2025-06-26"
    private String invoiceNo;           // e.g. "NLTE/2024-25/001"
    private String customerName;        // typo corrected to "customerName" below
    private String gstin;

    private LocalDate date = LocalDate.now(); // system date

    private List<SalesItem> items;
private String status;
    private double subtotal;
    private double cgst;
    private double sgst;
    private double igst;
    private double freight;
    private double total;
    private String poNo;         // e.g. "2025-06-26"
    private String poDate; 
}