package com.example.findpathserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // ⭐️ [추가]

@EnableScheduling
@SpringBootApplication
public class FindPathServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FindPathServerApplication.class, args);
	}

}
