package gr.aegean.deserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;

import gr.aegean.model.analysis.quality.QualityMetric;


class QualityMetricDeserializerTest {
    private QualityMetricDeserializer underTest;

    @BeforeEach
    void setup() {
        underTest = new QualityMetricDeserializer();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BUG_SEVERITY", "bUG_SEVeRITy", "  Bug  Severity  "})
    void shouldDeserializeQualityMetricIgnoringCaseAndSpaces(String metric) throws IOException {
        //Arrange
        QualityMetric expected = QualityMetric.BUG_SEVERITY;
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(metric);

        //Act
        QualityMetric actual = underTest.deserialize(parser, context);

        //Assert
        assertThat(actual).isEqualTo(expected);
    }

    /*
        Null case is covered by the @Valid annotation. There will be no mapping if the quality metric is null so no
        deserialization will happen for the quality metric property.
     */
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"invalidMetric"})
    void shouldThrowIllegalArgumentExceptionWhenQualityMetricIsInvalid(String metric) throws IOException {
        // Arrange
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(metric);

        //Act Assert
        assertThatThrownBy(() -> underTest.deserialize(parser, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid quality metric: " + metric.toUpperCase());
    }
}
