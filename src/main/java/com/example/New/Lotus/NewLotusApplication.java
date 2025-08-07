package com.example.New.Lotus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class NewLotusApplication {

	public static void main(String[] args) {
		System.out.println("Mongo URI: " + System.getenv("SPRING_DATA_MONGODB_URI"));
		SpringApplication.run(NewLotusApplication.class, args);
	}
	
	@Bean
	public WebMvcConfigurer corsConfigurer() {
	 return new WebMvcConfigurer() {
	     @Override
	     public void addCorsMappings(CorsRegistry registry) {
	         registry.addMapping("/**")
//	         .allowedOrigins("*")    all origins for testing (not recommended for production)
	            .allowedOrigins(
	                    "http://localhost:3001", 
	                    "https://my-app-frontend-1-7h0h.onrender.com"
	                )	                
	            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                 .allowedHeaders("*")
                 .allowCredentials(true);
	     }
	 };
	}


}

//src/main/java/com/yourpackage/InventoryApplication.java

