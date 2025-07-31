package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.services.IRedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class RedisService implements IRedisService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

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
}
