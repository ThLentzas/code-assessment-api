package gr.aegean.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import gr.aegean.model.analysis.quality.QualityMetric;

import java.io.IOException;

/*
    Handling cases where the provided quality attribute is in forms like ("Bug Severity", "  Bug  Severity").
    The deserializer converts the provided metric to uppercase, trims the input and replaces the " " with "_".
    It also covers cases like "Cyclomatic Complexity" to "CYCLOMATIC_COMPLEXITY" which in the enum value.
 */
public class MetricDeserializer extends JsonDeserializer<QualityMetric> {

    @Override
    public QualityMetric deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String metric = parser.getValueAsString()
                .trim()
                .replaceAll("\\s+", "_")
                .toUpperCase();

        try {
            return QualityMetric.valueOf(metric);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid quality metric: " + metric);
        }
    }
}
