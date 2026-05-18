package com.hcmute.careergraph.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
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
    // Ensure Jackson converter supports application/json with charset
    MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter(objectMapper);
    List<MediaType> supported = new ArrayList<>();
    supported.add(MediaType.APPLICATION_JSON);
    supported.add(MediaType.parseMediaType("application/json;charset=UTF-8"));
    supported.add(MediaType.APPLICATION_OCTET_STREAM);
    jackson.setSupportedMediaTypes(supported);

    // Put our converter at the beginning so it will be picked for JSON
    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
    converters.add(0, jackson);
  }
}
