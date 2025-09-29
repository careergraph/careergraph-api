package com.hcmute.careergraph.services;

public interface RedisService {

    <T> T getObject(String key, Class<T> clazz);

    <T> void setObject(String key, T value, Integer timeout);

    void deleteObject(String key);
}
