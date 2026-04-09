package com.bidcast.selection_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class SelectionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SelectionServiceApplication.class, args);
	}

}
