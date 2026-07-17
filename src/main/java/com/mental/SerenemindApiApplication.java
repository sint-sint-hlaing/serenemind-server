package com.mental;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class SerenemindApiApplication {

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Yangon"));
		System.out.println("Spring Boot Application covers TimeZone: " + TimeZone.getDefault().getID());
	}

	public static void main(String[] args) {
		SpringApplication.run(SerenemindApiApplication.class, args);
	}

}
