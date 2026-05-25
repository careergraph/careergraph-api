package com.hcmute.careergraph.config.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeminiDebug {

    @Value("${GEMINI_API_KEY:NOT_FOUND}")
    private String geminiKey;

    @PostConstruct
    public void init() {
        System.out.println("Gemini key = " + geminiKey);
    }
}