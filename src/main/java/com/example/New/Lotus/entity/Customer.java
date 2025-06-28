// com/example/New/Lotus/entity/Customer.java
package com.example.New.Lotus.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "customers")
@Data
public class Customer {
    @Id
    private String id;
    private String name;
    private String contact;
    private String address;
    private String gstin;
}
