package com.codesim;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class CodesimBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodesimBackendApplication.class, args);
	}

}
