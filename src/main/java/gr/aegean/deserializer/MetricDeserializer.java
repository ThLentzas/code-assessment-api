package gr.aegean.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import gr.aegean.model.analysis.quality.QualityMetric;

import java.io.IOException;


public class MetricDeserializer extends JsonDeserializer<QualityMetric> {

    @Override
    public QualityMetric deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String metric = parser.getValueAsString().toUpperCase();

        try {
            return QualityMetric.valueOf(metric);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid quality metric: " + metric);
        }
    }
}
