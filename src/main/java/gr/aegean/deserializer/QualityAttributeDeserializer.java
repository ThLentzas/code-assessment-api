package gr.aegean.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import gr.aegean.model.analysis.quality.QualityAttribute;

import java.io.IOException;

/*
    Handling cases where the provided quality attribute is in forms like ("Bug Severity", "  Bug  Severity").
    The deserializer converts the provided attribute to uppercase, trims the input and replaces one or more " "
    with "_". It covers cases like "Hotspot Priority" to "HOTSPOT_PRIORITY" which in the enum value.
 */
public class QualityAttributeDeserializer extends JsonDeserializer<QualityAttribute> {

    @Override
    public QualityAttribute deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String attribute = parser.getValueAsString()
                .trim()
                .replaceAll("\\s+", "_")
                .toUpperCase();

        try {
            return QualityAttribute.valueOf(attribute);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid quality attribute: " + attribute);
        }
    }
}
