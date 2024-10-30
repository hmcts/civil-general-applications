package uk.gov.hmcts.reform.civil.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleCaseDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void whenCreatingSimpleCaseDetails_thenShouldHaveCorrectId() {
        Long expectedId = 123L;

        SimpleCaseDetails simpleCaseDetails = SimpleCaseDetails.builder()
            .id(expectedId)
            .build();

        assertThat(simpleCaseDetails.getId()).isEqualTo(expectedId);
    }

    @Test
    void whenUsingToBuilder_thenShouldModifyId() {
        SimpleCaseDetails original = SimpleCaseDetails.builder()
            .id(123L)
            .build();

        SimpleCaseDetails modified = original.toBuilder()
            .id(456L)
            .build();

        assertThat(original.getId()).isEqualTo(123L);
        assertThat(modified.getId()).isEqualTo(456L);
    }

    @Test
    void whenSerializingToJson_thenShouldMatchExpectedJson() throws Exception {
        SimpleCaseDetails simpleCaseDetails = SimpleCaseDetails.builder()
            .id(123L)
            .build();
        String expectedJson = "{\"id\":123}";

        String jsonResult = objectMapper.writeValueAsString(simpleCaseDetails);

        assertThat(jsonResult).isEqualToIgnoringWhitespace(expectedJson);
    }

    @Test
    void whenDeserializingFromJson_thenShouldCreateCorrectObject() throws Exception {
        String json = "{\"id\":123}";

        SimpleCaseDetails result = objectMapper.readValue(json, SimpleCaseDetails.class);

        assertThat(result.getId()).isEqualTo(123L);
    }

    @Test
    void whenComparingEqualObjects_thenEqualsShouldReturnTrue() {
        SimpleCaseDetails first = SimpleCaseDetails.builder().id(123L).build();
        SimpleCaseDetails second = SimpleCaseDetails.builder().id(123L).build();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void whenComparingDifferentObjects_thenNotEqualsShouldReturnTrue() {
        SimpleCaseDetails first = SimpleCaseDetails.builder().id(123L).build();
        SimpleCaseDetails second = SimpleCaseDetails.builder().id(456L).build();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void whenSettingIdAsNull_thenObjectShouldHandleNull() {
        SimpleCaseDetails simpleCaseDetails = SimpleCaseDetails.builder()
            .id(null)
            .build();

        assertThat(simpleCaseDetails.getId()).isNull();
    }

    @Test
    void whenUsingToString_thenShouldIncludeId() {
        SimpleCaseDetails simpleCaseDetails = SimpleCaseDetails.builder()
            .id(123L)
            .build();

        assertThat(simpleCaseDetails.toString()).contains("id=123");
    }

    @Test
    void whenUsingBuilderWithNoValues_thenObjectShouldBeCreatedWithDefaults() {
        SimpleCaseDetails simpleCaseDetails = SimpleCaseDetails.builder().build();

        assertThat(simpleCaseDetails).isNotNull();
        assertThat(simpleCaseDetails.getId()).isNull();
    }

    @Test
    void whenCopyingWithToBuilderWithoutChanges_thenShouldReturnIdenticalObject() {
        SimpleCaseDetails original = SimpleCaseDetails.builder().id(123L).build();

        SimpleCaseDetails copy = original.toBuilder().build();

        assertThat(copy).isEqualTo(original);
        assertThat(copy.getId()).isEqualTo(original.getId());
    }

}
