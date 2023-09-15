package gr.aegean.config;

import gr.aegean.deserializer.AttributeDeserializer;
import gr.aegean.model.analysis.quality.QualityAttribute;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import gr.aegean.deserializer.OperatorDeserializer;
import gr.aegean.deserializer.MetricDeserializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;


@Configuration
public class DeserializerConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addDeserializer(QualityMetric.class, new MetricDeserializer());
        module.addDeserializer(QualityAttribute.class, new AttributeDeserializer());
        module.addDeserializer(QualityMetricOperator.class, new OperatorDeserializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }
}