package com.bidcast.auction_service;

import org.springframework.boot.SpringApplication;

public class TestAuctionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(AuctionServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
