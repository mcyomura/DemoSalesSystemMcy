package com.salessystem.customerauthservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enables background scheduled tasks for key rotation
public class CustomerAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerAuthServiceApplication.class, args);
	}

}
