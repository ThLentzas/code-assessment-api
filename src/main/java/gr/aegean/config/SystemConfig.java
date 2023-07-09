package gr.aegean.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemConfig {

    @Bean
    public ProcessBuilder processBuilder() {
        return new ProcessBuilder();
    }
}
