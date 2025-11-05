package com.okebari.artbite.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager("userDetails");
		cacheManager.setCaffeine(Caffeine.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES) // 10분 후 만료
			.maximumSize(500) // 최대 500개 항목 저장
			.recordStats()); // 캐시 통계 기록
		return cacheManager;
	}
}
