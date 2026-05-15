package com.lklass;

import org.springframework.boot.SpringApplication;

public class TestLklassApplication {

	public static void main(String[] args) {
		SpringApplication.from(LklassApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
