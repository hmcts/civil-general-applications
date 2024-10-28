package uk.gov.hmcts.reform.civil.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleCaseDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void whenCreatingSimpleCaseDetails_thenShouldHaveCorrectId() {
        // Given
        Long expectedId = 123L;

        // When
        SimpleCaseDetails simpleCaseDetails = SimpleCaseDetails.builder()
            .id(expectedId)
            .build();

        // Then
        assertThat(simpleCaseDetails.getId()).isEqualTo(expectedId);
    }

    @Test
    public void whenUsingToBuilder_thenShouldModifyId() {
        // Given
        SimpleCaseDetails original = SimpleCaseDetails.builder()
            .id(123L)
            .build();

        // When
        SimpleCaseDetails modified = original.toBuilder()
            .id(456L)
            .build();

        // Then
        assertThat(original.getId()).isEqualTo(123L);
        assertThat(modified.getId()).isEqualTo(456L);
    }

    @Test
    public void whenSerializingToJson_thenShouldMatchExpectedJson() throws Exception {
        // Given
        SimpleCaseDetails simpleCaseDetails = SimpleCaseDetails.builder()
            .id(123L)
            .build();
        String expectedJson = "{\"id\":123}";

        // When
        String jsonResult = objectMapper.writeValueAsString(simpleCaseDetails);

        // Then
        assertThat(jsonResult).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    public void whenDeserializingFromJson_thenShouldCreateCorrectObject() throws Exception {
        // Given
        String json = "{\"id\":123}";

        // When
        SimpleCaseDetails result = objectMapper.readValue(json, SimpleCaseDetails.class);

        // Then
        assertThat(result.getId()).isEqualTo(123L);
    }

    @Test
    public void whenComparingEqualObjects_thenEqualsShouldReturnTrue() {
        // Given
        SimpleCaseDetails first = SimpleCaseDetails.builder().id(123L).build();
        SimpleCaseDetails second = SimpleCaseDetails.builder().id(123L).build();

        // Then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    public void whenComparingDifferentObjects_thenEqualsShouldReturnFalse() {
        // Given
        SimpleCaseDetails first = SimpleCaseDetails.builder().id(123L).build();
        SimpleCaseDetails second = SimpleCaseDetails.builder().id(456L).build();

        // Then
        assertThat(first).isNotEqualTo(second);
    }
}