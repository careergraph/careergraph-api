package com.hcmute.careergraph.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisServiceImpl implements com.hcmute.careergraph.services.RedisService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refreshable-duration}")
    private Integer refreshTtl;

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        String json = (String) redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new InternalException("Do not get object: " + e.getMessage());
        }
    }

    @Override
    public <T> void setObject(String key, T value, Integer timeout) {
        if (value == null) {
            throw new InternalException("Do not set object");
        }

        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(timeout));
        } catch (JsonProcessingException e) {
            throw new InternalException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public void deleteObject(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public void setField(String key, String field, Object value) {
        Map<String, Object> meta = getObject(key, Map.class);
        if (meta == null) meta = new HashMap<>();

        // CẬP NHẬT field
        meta.put(field, value);

        // GIỮ TTL cũ (nếu có)
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        int seconds = (ttl == null || ttl < 0) ? refreshTtl : ttl.intValue();
        // GHI lại object
        setObject(key, meta, seconds);
    }

    @Override
    public <T> T getField(String key, String field, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value.toString(), clazz);
        } catch (JsonProcessingException e) {
            throw new InternalException("Failed to deserialize field value", e);
        }
    }
}
