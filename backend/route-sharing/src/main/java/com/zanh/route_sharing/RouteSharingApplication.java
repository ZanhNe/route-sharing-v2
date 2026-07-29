package com.zanh.route_sharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RouteSharingApplication {

	public static void main(String[] args) {
		SpringApplication.run(RouteSharingApplication.class, args);
	}

}
