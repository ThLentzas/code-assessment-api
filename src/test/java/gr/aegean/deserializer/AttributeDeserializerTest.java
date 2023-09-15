package gr.aegean.deserializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import gr.aegean.model.analysis.quality.QualityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class AttributeDeserializerTest {
    private AttributeDeserializer underTest;

    @BeforeEach
    void setup() {
        underTest = new AttributeDeserializer();
    }

    @Test
    void shouldDeserializeAttribute() throws IOException {
        // Arrange
        String expected = "RELIABILITY";
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(expected);

        // Act
        QualityAttribute actual = underTest.deserialize(parser, context);

        // Assert
        assertThat(actual).isEqualTo(QualityAttribute.valueOf(expected));
    }

    @Test
    void shouldDeserializeAttributeIgnoringCaseAndExtraSpaces() throws IOException {
        // Arrange
        String input = "  Bug  Severity  ";
        String expected = "BUG_SEVERITY";
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(input);

        // Act
        QualityAttribute actual = underTest.deserialize(parser, context);

        // Assert
        assertThat(actual).isEqualTo(QualityAttribute.valueOf(expected));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAttributeIsInvalid() throws IOException {
        // Arrange
        String attribute = "invalidAttribute";
        JsonParser parser = mock(JsonParser.class);
        DeserializationContext context = mock(DeserializationContext.class);

        when(parser.getValueAsString()).thenReturn(attribute);

        // Assert
        assertThatThrownBy(() -> underTest.deserialize(parser, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid quality attribute: " + attribute.toUpperCase());
    }


}
