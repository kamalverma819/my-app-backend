package com.example.New.Lotus.Model;
//File: InvoicePdfRequest.java

import java.util.List;

public class InvoicePdfRequest {
 public String invoiceNo;
 public String invoiceDate;
 public String poNo;
 public String poDate;
 public Customer customer;
 public CompanyInfo company;
 public List<ItemLine> items;
 public double subtotal;
 public double freight;
 public double cgst;
 public double sgst;
 public double igst;
 public double total;
 public String amountInWords;

 public static class Customer {
     public String name;
     public String gstin;
 }

 public static class CompanyInfo {
     public String name;
     public String address;
     public String gstin;
     public String contact;
     public String email;
     public Bank bank;
     public String declaration;
     public String footer;

     public static class Bank {
         public String name;
         public String branch;
         public String account;
         public String ifsc;
         public String branchCode;
     }
 }

 public static class ItemLine {
     public String name;
     public String hsnCode;
     public int quantity;
     public double price;
     public double discount;
 }
}

