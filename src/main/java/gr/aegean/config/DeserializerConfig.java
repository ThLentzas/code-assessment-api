package gr.aegean.config;

import gr.aegean.deserializer.QualityAttributeDeserializer;
import gr.aegean.model.analysis.quality.QualityAttribute;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import gr.aegean.deserializer.QualityMetricOperatorDeserializer;
import gr.aegean.deserializer.QualityMetricDeserializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ObjectMapper;


@Configuration
public class DeserializerConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addDeserializer(QualityMetric.class, new QualityMetricDeserializer());
        module.addDeserializer(QualityAttribute.class, new QualityAttributeDeserializer());
        module.addDeserializer(QualityMetricOperator.class, new QualityMetricOperatorDeserializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }
}