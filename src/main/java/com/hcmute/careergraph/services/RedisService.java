package com.hcmute.careergraph.services;

public interface RedisService {

    <T> T getObject(String key, Class<T> clazz);

    <T> void setObject(String key, T value, Integer timeout);

    void deleteObject(String key);
    boolean exists(String key);
    void setField(String key, String field, Object value);
    <T> T getField(String key, String field, Class<T> clazz);
}
