package io.github.saiharshith.ordermanagementplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrderManagementPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderManagementPlatformApplication.class, args);
	}

}
