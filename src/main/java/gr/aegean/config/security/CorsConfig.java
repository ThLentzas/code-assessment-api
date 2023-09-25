package gr.aegean.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
public class CorsConfig {
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers}")
    private String exposedHeaders;

    /*
        We have to expose the Location header to the FE, so that they can have access of the newly created resource.
        "Best Practices for CORS: NEVER deactivate and NEVER Set Allowed Origins (*). Always explicitly define values
        from .properties or .yaml file, do not hardcode"
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        /*
            If there's no comma in the string, the split(",") method will return an array containing the original
            string as its only element. That's the case we only have a single value.
         */
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of(allowedMethods.split(",")));
        configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));
        configuration.setExposedHeaders(List.of(exposedHeaders.split(",")));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
