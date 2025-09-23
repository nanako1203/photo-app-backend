package com.caihuan.photo_app_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PhotoAppBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhotoAppBackendApplication.class, args);
	}

}
		