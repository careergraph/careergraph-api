package com.hcmute.careergraph.entities.base;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

public interface UUIDEntity {

    String getUUID();

    void setUUID(String uuid);

    default String generateUUID() {
        if (getUUID() == null) {
            char[] chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
            return NanoIdUtils.randomNanoId(NanoIdUtils.DEFAULT_NUMBER_GENERATOR, chars, 16);
        }

        return getUUID();
    }
}
