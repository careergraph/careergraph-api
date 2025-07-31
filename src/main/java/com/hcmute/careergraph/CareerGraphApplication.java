package com.hcmute.careergraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CareerGraphApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareerGraphApplication.class, args);
	}
}
