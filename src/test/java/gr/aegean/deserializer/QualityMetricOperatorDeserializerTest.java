package gr.aegean.deserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import gr.aegean.model.analysis.quality.QualityMetricOperator;

import java.io.IOException;


class QualityMetricOperatorDeserializerTest {
    private QualityMetricOperatorDeserializer underTest;

    @BeforeEach
    void setup() {
        underTest = new QualityMetricOperatorDeserializer();
    }

    @Test
    void shouldDeserializeSymbolToQualityMetricOperator() throws IOException {
        //Arrange
        String symbol = "<=";
        QualityMetricOperator expected = QualityMetricOperator.LTE;
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(symbol);

        //Act
        QualityMetricOperator actual = underTest.deserialize(parser, context);

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    /*
        Null case is covered by the @Valid annotation. There will be no mapping if the quality metric operator is null
        so no deserialization will happen for the quality metric operator property.
     */
    @ParameterizedTest
    @ValueSource(strings = {"("})
    @EmptySource
    void shouldThrowIllegalArgumentExceptionWhenSymbolIsInvalid(String symbol) throws IOException {
        // Arrange
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(symbol);

        // Act Assert
        assertThatThrownBy(() -> underTest.deserialize(parser, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid operator symbol: " + symbol);
    }
}
