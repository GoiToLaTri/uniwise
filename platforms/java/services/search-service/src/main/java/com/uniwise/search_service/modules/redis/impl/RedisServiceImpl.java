package com.uniwise.search_service.modules.redis.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwise.search_service.modules.redis.RedisService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisServiceImpl implements RedisService {
    private static final long SCAN_COUNT = 200L;
    private static final int DELETE_BATCH_SIZE = 100;

    RedisTemplate<String, String> template;
    ObjectMapper objectMapper;

    @Override
    public <T> T getKey(String key, Class<T> clazz) {
        String jsonValue = template.opsForValue().get(key);
        if (jsonValue == null || jsonValue.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonValue, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to deserialize Redis value", e);
        }
    }

    @Override
    public <T> T getKey(String key, TypeReference<T> typeRef) {
        String jsonValue = template.opsForValue().get(key);
        if (jsonValue == null || jsonValue.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonValue, typeRef);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to deserialize Redis value", e);
        }
    }

    @Override
    public <T> void setKey(String key, T value, Long ttl, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            template.opsForValue().set(key, jsonValue, Duration.of(ttl, timeUnit.toChronoUnit()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to serialize value for Redis", e);
        }
    }

    @Override
    public void deleteKey(String key) {
        Boolean deleted = template.delete(key);
        if (Boolean.TRUE.equals(deleted))
            log.info("Deleted key {}", key);
        else
            log.warn("Cannot delete key {}", key);
    }

    @Override
    public void deleteMultipleKey(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        template.delete(keys);
    }

    @Override
    public void deleteKeysByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_COUNT)
                .build();

        int totalDeleted = 0;
        List<String> batch = new ArrayList<>(DELETE_BATCH_SIZE);

        try (Cursor<String> cursor = template.scan(options)) {
            if (cursor == null) {
                return;
            }
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= DELETE_BATCH_SIZE) {
                    template.delete(List.copyOf(batch));
                    totalDeleted += batch.size();
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            template.delete(List.copyOf(batch));
            totalDeleted += batch.size();
        }

        if (totalDeleted > 0) {
            log.info("Deleted {} keys matching pattern {}", totalDeleted, pattern);
        }
    }
}
