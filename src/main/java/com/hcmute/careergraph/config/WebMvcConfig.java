package com.hcmute.careergraph.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final ObjectMapper objectMapper;

  public WebMvcConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    // Preserve Spring Boot's default Jackson media types such as the actuator
    // vendor types, and only append the legacy JSON charset variant we need.
    for (HttpMessageConverter<?> converter : converters) {
      if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
        jackson.setObjectMapper(objectMapper);

        List<MediaType> supported = new ArrayList<>(jackson.getSupportedMediaTypes());
        MediaType legacyJsonUtf8 = MediaType.parseMediaType("application/json;charset=UTF-8");

        if (!supported.contains(MediaType.APPLICATION_JSON)) {
          supported.add(MediaType.APPLICATION_JSON);
        }
        if (!supported.contains(legacyJsonUtf8)) {
          supported.add(legacyJsonUtf8);
        }

        jackson.setSupportedMediaTypes(supported);
      }
    }
  }
}
