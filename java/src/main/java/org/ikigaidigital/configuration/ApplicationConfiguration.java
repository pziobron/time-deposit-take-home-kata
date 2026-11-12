package org.ikigaidigital.configuration;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.application.service.TimeDepositService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Wires application dependencies and local Swagger UI browser access. */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {
    @Bean
    TimeDepositService timeDepositService(TimeDepositRepository repository) {
        return new TimeDepositService(repository, new TimeDepositCalculator());
    }

    @Bean
    WebMvcConfigurer swaggerCorsConfiguration() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/time-deposits/**")
                    .allowedOrigins("http://localhost:8081")
                    .allowedMethods("GET", "POST");
            }
        };
    }
}
