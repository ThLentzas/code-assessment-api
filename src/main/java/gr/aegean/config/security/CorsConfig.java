package gr.aegean.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.validation.constraints.NotNull;

@Configuration
public class CorsConfig {

    /*
        We have to expose the Location header to the FE, so that they can have access of the newly created resource.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("*")
                        .exposedHeaders("Location");
            }
        };
    }
}
