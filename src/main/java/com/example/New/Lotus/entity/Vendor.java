package com.example.New.Lotus.entity;
import 	jakarta.validation.constraints.NotBlank;
import 	jakarta.validation.constraints.Pattern;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "vendors")
@Data
public class Vendor {
    @Id
    private String id;
    
    @NotBlank(message = "Vendor name is required")
    private String name;
    
//    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", 
//             message = "Invalid GSTIN format")
    private String gstin;
    
    @NotBlank(message = "Contact is required")
    private String contact;
    
    private String address;
}