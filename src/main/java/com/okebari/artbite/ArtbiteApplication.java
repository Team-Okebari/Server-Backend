package com.okebari.artbite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing // JPA Auditing 활성화
@EnableScheduling // 스케줄러 활성화
@SpringBootApplication
public class ArtbiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArtbiteApplication.class, args);
	}

}
