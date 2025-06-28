package com.example.New.Lotus.Repository;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.New.Lotus.Model.Item;

//src/main/java/com/yourpackage/repository/ItemRepository.java
public interface ItemRepository extends MongoRepository<Item, String> {
	
	Optional<Item> findByNameAndHsnCode(String name, String hsn);
}
