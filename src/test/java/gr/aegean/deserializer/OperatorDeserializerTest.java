package gr.aegean.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import gr.aegean.model.analysis.quality.QualityMetricOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatorDeserializerTest {
    private OperatorDeserializer underTest;

    @BeforeEach
    void setup() {
        underTest = new OperatorDeserializer();
    }

    @Test
    void shouldDeserializeSymbolToOperator() throws IOException {
        String symbol = "<=";
        QualityMetricOperator expected = QualityMetricOperator.LTE;
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(symbol);

        QualityMetricOperator actual = underTest.deserialize(parser, context);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSymbolIsInvalid() throws IOException {
        String symbol = "://";
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(symbol);

        assertThatThrownBy(() -> underTest.deserialize(parser, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid operator symbol: " + symbol);
    }
}
