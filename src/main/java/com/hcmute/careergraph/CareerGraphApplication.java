package com.hcmute.careergraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync  // V2.1: Enable async support for FileEventListener
public class CareerGraphApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareerGraphApplication.class, args);
	}
}
