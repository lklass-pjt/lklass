package com.lklass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class LklassApplication {

	public static void main(String[] args) {
		SpringApplication.run(LklassApplication.class, args);
	}

}
