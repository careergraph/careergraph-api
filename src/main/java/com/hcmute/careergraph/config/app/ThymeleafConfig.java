package com.hcmute.careergraph.config.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;

@Configuration
public class ThymeleafConfig {

   @Bean
   public SpringTemplateEngine springTemplateEngine() {
      SpringTemplateEngine templateEngine = new SpringTemplateEngine();
      templateEngine.addTemplateResolver(htmlTemplateResolver());
      templateEngine.setEnableSpringELCompiler(false);
      return templateEngine;
   }

   @Bean
   public SpringResourceTemplateResolver htmlTemplateResolver(){
      SpringResourceTemplateResolver emailTemplateResolver = new SpringResourceTemplateResolver();
      emailTemplateResolver.setPrefix("classpath:/templates/");
      emailTemplateResolver.setSuffix(".html");
      emailTemplateResolver.setTemplateMode(TemplateMode.HTML);
      emailTemplateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
      emailTemplateResolver.setCacheable(true);
      emailTemplateResolver.setCheckExistence(true);
      return emailTemplateResolver;
   }
}
