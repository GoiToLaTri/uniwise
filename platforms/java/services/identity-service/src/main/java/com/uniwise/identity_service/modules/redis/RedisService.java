package com.uniwise.identity_service.modules.redis;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface RedisService {

    <T> void setKey(String key, T value, Long ttl, TimeUnit timeUnit);

    <T> T getKey(String key, Class<T> clazz);

    void deleteKey(String key);

    void deleteMultipleKey(List<String> keys);
}
