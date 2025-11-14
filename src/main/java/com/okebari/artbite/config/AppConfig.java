package com.okebari.artbite.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.okebari.artbite.note.config.NoteReminderProperties;
import com.okebari.artbite.payment.toss.config.TossPaymentConfig;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NoteReminderProperties.class)
public class AppConfig {

	private final TossPaymentConfig tossPaymentConfig;

	public AppConfig(TossPaymentConfig tossPaymentConfig) {
		this.tossPaymentConfig = tossPaymentConfig;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Clock koreaClock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}
}
