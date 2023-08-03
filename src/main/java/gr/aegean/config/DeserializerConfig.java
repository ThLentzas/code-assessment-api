package gr.aegean.config;

import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import gr.aegean.deserializer.OperatorDeserializer;
import gr.aegean.deserializer.MetricDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DeserializerConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addDeserializer(QualityMetricOperator.class, new OperatorDeserializer());
        module.addDeserializer(QualityMetric.class, new MetricDeserializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }
}