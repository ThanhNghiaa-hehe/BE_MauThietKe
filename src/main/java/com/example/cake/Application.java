package com.example.cake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.example.cake")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
