package com.okebari.artbite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.okebari.artbite.payment.toss.config.TossPaymentConfig;

@Configuration
@EnableScheduling
public class AppConfig {

	private final TossPaymentConfig tossPaymentConfig;

	public AppConfig(TossPaymentConfig tossPaymentConfig) {
		this.tossPaymentConfig = tossPaymentConfig;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean // Add RestTemplate bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
