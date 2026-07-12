package com.uniwise.search_service.modules.redis.impl;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
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
        java.util.Set<String> keys = template.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            template.delete(keys);
            log.info("Deleted {} keys matching pattern {}", keys.size(), pattern);
        }
    }
}
