package com.uniwise.search_service.modules.redis;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.type.TypeReference;

public interface RedisService {

    <T> void setKey(String key, T value, Long ttl, TimeUnit timeUnit);

    <T> T getKey(String key, Class<T> clazz);

    <T> T getKey(String key, TypeReference<T> typeRef);

    void deleteKey(String key);

    void deleteMultipleKey(List<String> keys);

    void deleteKeysByPattern(String pattern);
}
