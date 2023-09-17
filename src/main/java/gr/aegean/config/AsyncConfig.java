package gr.aegean.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;


/*
    Having those properties would help us in the future for more threads when we have better hardware
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    @Value("${thread.corePoolSize}")
    private int corePoolSize;
    @Value("${thread.maxPoolSize}")
    private int maxPoolSize;

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("CodeAssessment-");
        executor.initialize();

        return executor;
    }
}
