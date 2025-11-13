package com.okebari.artbite.note.service.support;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okebari.artbite.note.config.NoteReminderProperties;
import com.okebari.artbite.note.domain.NoteReminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기존 `NoteReminderCachePort` 로직을 구현체로 분리한 클래스.
 * 자정 워밍/상태 전이 시점에 필요한 SETNX + TTL + 파이프라이닝을 그대로 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisReminderCacheClient implements ReminderCacheClient {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
	private final NoteReminderProperties properties;

	@Override
	public Optional<NoteReminderCacheValue> get(Long userId, LocalDate date) {
		String key = buildKey(userId, date);
		String json = redisTemplate.opsForValue().get(key);
		if (json == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, NoteReminderCacheValue.class));
		} catch (JsonProcessingException e) {
			log.warn("Failed to deserialize reminder cache key={}", key, e);
			return Optional.empty();
		}
	}

	@Override
	public void save(NoteReminder reminder) {
		saveAll(List.of(reminder));
	}

	@Override
	public void saveAll(Collection<NoteReminder> reminders) {
		if (reminders == null || reminders.isEmpty()) {
			return;
		}
		Duration ttl = Duration.ofHours(properties.getRedisTtlHours());
		int ttlSeconds = (int) ttl.toSeconds();

		redisTemplate.executePipelined(new RedisCallback<Void>() {
			@Override
			public Void doInRedis(RedisConnection connection) throws DataAccessException {
				reminders.forEach(reminder -> {
					try {
						String key = buildKey(reminder.getUserId(), reminder.getReminderDate());
						String value = objectMapper.writeValueAsString(NoteReminderCacheValue.from(reminder));
						byte[] keyBytes = redisTemplate.getStringSerializer().serialize(key);
						byte[] valueBytes = redisTemplate.getStringSerializer().serialize(value);
						if (keyBytes != null && valueBytes != null) {
							connection.stringCommands().setEx(keyBytes, ttlSeconds, valueBytes);
						}
					} catch (JsonProcessingException e) {
						log.warn("Failed to serialize reminder cache userId={} date={}",
							reminder.getUserId(), reminder.getReminderDate(), e);
					}
				});
				return null;
			}
		});
	}

	@Override
	public void evict(Long userId, LocalDate date) {
		redisTemplate.delete(buildKey(userId, date));
	}

	private String buildKey(Long userId, LocalDate date) {
		return "note:reminder:%s:%s".formatted(userId, date);
	}
}
