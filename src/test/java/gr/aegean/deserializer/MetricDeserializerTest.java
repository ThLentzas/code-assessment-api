package gr.aegean.deserializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gr.aegean.model.analysis.quality.QualityMetric;

import java.io.IOException;


class MetricDeserializerTest {
    private MetricDeserializer underTest;

    @BeforeEach
    void setup() {
        underTest = new MetricDeserializer();
    }

    @Test
    void shouldDeserializeMetric() throws IOException {
        //Arrange
        String expected = "DUPLICATION";
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(expected.toUpperCase());

        //Act
        QualityMetric actual = underTest.deserialize(parser, context);

        //Assert
        assertThat(actual).isEqualTo(QualityMetric.valueOf(expected));
    }

    @Test
    void shouldDeserializeMetricIgnoringCase() throws IOException {
        //Arrange
        String expected = "DupLIcatioN";
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(expected.toUpperCase());

        //Act
        QualityMetric actual = underTest.deserialize(parser, context);

        //Assert
        assertThat(actual).isEqualTo(QualityMetric.valueOf(expected.toUpperCase()));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMetricIsInvalid() throws IOException {
        //Arrange
        String metric = "invalidMetric";
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        //Act
        when(parser.getValueAsString()).thenReturn(metric.toUpperCase());

        //Assert
        assertThatThrownBy(() -> underTest.deserialize(parser, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid quality metric: " + metric.toUpperCase());
    }
}
