package gr.aegean.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import gr.aegean.model.analysis.quality.QualityMetricOperator;

import java.io.IOException;

/*
    Receives an equality symbol and maps it to an Enum value. Valid symbols are: >, >=, <, <=, ==, <>.
 */
public class OperatorDeserializer extends JsonDeserializer<QualityMetricOperator> {

    @Override
    public QualityMetricOperator deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String symbol = parser.getValueAsString();

        /*
            Converts the symbol to the enum value.
         */
        for (QualityMetricOperator operator : QualityMetricOperator.values()) {
            if (operator.getSymbol().equals(symbol)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Invalid operator symbol: " + symbol);
    }
}