package uk.gov.hmcts.reform.civil.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleCallbackRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void whenBuildingSimpleCallbackRequest_thenShouldCreateValidInstance() {
        SimpleCaseDetails caseDetails = SimpleCaseDetails.builder()
            .id(1L)
            .build();

        SimpleCallbackRequest request = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails)
            .build();

        assertThat(request).isNotNull();
        assertThat(request.getCaseDetails()).isEqualTo(caseDetails);
    }

    @Test
    void whenSerializingSimpleCallbackRequest_thenShouldReturnCorrectJson() throws JsonProcessingException {
        SimpleCaseDetails caseDetails = SimpleCaseDetails.builder()
            .id(1L)
            .build();

        SimpleCallbackRequest request = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails)
            .build();

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"case_details\"");
        assertThat(json).contains("\"id\":1");
    }

    @Test
    void whenDeserializingJson_thenShouldReturnCorrectSimpleCallbackRequest() throws JsonProcessingException {
        String json = "{\"case_details\":{\"id\":1}}";

        SimpleCallbackRequest request = objectMapper.readValue(json, SimpleCallbackRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getCaseDetails()).isNotNull();
        assertThat(request.getCaseDetails().getId()).isEqualTo(1L);
    }

    @Test
    void whenEqualsAndHashCodeAreCalled_thenShouldBehaveCorrectly() {
        SimpleCaseDetails caseDetails1 = SimpleCaseDetails.builder().id(1L).build();
        SimpleCaseDetails caseDetails2 = SimpleCaseDetails.builder().id(1L).build();

        SimpleCallbackRequest request1 = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails1)
            .build();
        SimpleCallbackRequest request2 = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails2)
            .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());

        request2.setCaseDetails(SimpleCaseDetails.builder().id(2L).build());

        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void whenCaseDetailsIsNull_thenShouldHandleCorrectly() {
        SimpleCallbackRequest request = SimpleCallbackRequest.builder()
            .caseDetails(null)
            .build();

        assertThat(request).isNotNull();
        assertThat(request.getCaseDetails()).isNull();
    }

    @Test
    void whenDeserializingEmptyJson_thenShouldReturnRequestWithNullCaseDetails() throws JsonProcessingException {
        String json = "{}";

        SimpleCallbackRequest request = objectMapper.readValue(json, SimpleCallbackRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getCaseDetails()).isNull();
    }

    @Test
    void whenUsingToString_thenShouldReturnNonEmptyString() {
        SimpleCaseDetails caseDetails = SimpleCaseDetails.builder().id(1L).build();
        SimpleCallbackRequest request = SimpleCallbackRequest.builder()
            .caseDetails(caseDetails)
            .build();

        String toStringOutput = request.toString();

        assertThat(toStringOutput).isNotNull();
        assertThat(toStringOutput).contains("SimpleCallbackRequest");
        assertThat(toStringOutput).contains("caseDetails=SimpleCaseDetails(id=1)");
    }
}
