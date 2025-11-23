package com.document.notification.system.outbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 12/13/2024
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    // esto solo lo colocariamos si necesitamos modificar el modulo del Mapper Json
    /**
     @Bean
     @Primary
     public ObjectMapper objectMapper(){
     return new ObjectMapper()
     .setSerializationInclusion(JsonInclude.Include.NON_NULL)
     .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
     .registerModule(new JavaTimeModule());
     }
     */
}