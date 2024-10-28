package uk.gov.hmcts.reform.civil.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleCallbackRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        // Initialize ObjectMapper for JSON serialization/deserialization
        objectMapper = new ObjectMapper();
    }

    @Test
    public void whenBuildingSimpleCallbackRequest_thenShouldCreateValidInstance() {
        // Arrange
        SimpleCaseDetails caseDetails = SimpleCaseDetails.builder()
            .id(1L)
            .build();

        // Act
        SimpleCallbackRequest request = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails)
            .build();

        // Assert
        assertThat(request).isNotNull();
        assertThat(request.getCaseDetails()).isEqualTo(caseDetails);
    }

    @Test
    public void whenSerializingSimpleCallbackRequest_thenShouldReturnCorrectJson() throws JsonProcessingException {
        // Arrange
        SimpleCaseDetails caseDetails = SimpleCaseDetails.builder()
            .id(1L)
            .build();

        SimpleCallbackRequest request = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails)
            .build();

        // Act
        String json = objectMapper.writeValueAsString(request);

        // Assert
        assertThat(json).contains("\"case_details\"");
        assertThat(json).contains("\"id\":1");
    }

    @Test
    public void whenDeserializingJson_thenShouldReturnCorrectSimpleCallbackRequest() throws JsonProcessingException {
        // Arrange
        String json = "{\"case_details\":{\"id\":1}}";

        // Act
        SimpleCallbackRequest request = objectMapper.readValue(json, SimpleCallbackRequest.class);

        // Assert
        assertThat(request).isNotNull();
        assertThat(request.getCaseDetails()).isNotNull();
        assertThat(request.getCaseDetails().getId()).isEqualTo(1L);
    }

    @Test
    public void whenEqualsAndHashCodeAreCalled_thenShouldBehaveCorrectly() {
        // Arrange
        SimpleCaseDetails caseDetails1 = SimpleCaseDetails.builder().id(1L).build();
        SimpleCaseDetails caseDetails2 = SimpleCaseDetails.builder().id(1L).build();

        SimpleCallbackRequest request1 = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails1)
            .build();
        SimpleCallbackRequest request2 = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails2)
            .build();

        // Act & Assert
        assertThat(request1).isEqualTo(request2); // They should be equal
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode()); // Hash codes should be equal

        // Change case details
        request2.setCaseDetails(SimpleCaseDetails.builder().id(2L).build());

        // Assert that they are now different
        assertThat(request1).isNotEqualTo(request2);
    }
}

