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
}
