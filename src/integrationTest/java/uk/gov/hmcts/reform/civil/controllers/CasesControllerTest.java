package uk.gov.hmcts.reform.civil.controllers;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.citizenui.dto.EventDto;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.citizen.events.CaseEventService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CasesControllerTest extends BaseIntegrationTest {

    protected static final String BEARER_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92VnYxK3k"
        + "rV2dySDVVaTlXVGlvTHQwPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJzb2xpY2l0b3JAZXhhbXBsZS5jb20iLCJhdXRoX2xldmVsIjowLC"
        + "JhdWRpdFRyYWNraW5nSWQiOiJiNGJmMjJhMi02ZDFkLTRlYzYtODhlOS1mY2NhZDY2NjM2ZjgiLCJpc3MiOiJodHRwOi8vZnItYW06ODA4M"
        + "C9vcGVuYW0vb2F1dGgyL2htY3RzIiwidG9rZW5OYW1lIjoiYWNjZXNzX3Rva2VuIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImF1dGhHcmFu"
        + "dElkIjoiZjExMTk3MGQtMzQ3MS00YjY3LTkxMzYtZmYxYzk0MjMzMTZmIiwiYXVkIjoieHVpX3dlYmFwcCIsIm5iZiI6MTU5NDE5NzI3NCw"
        + "iZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJyb2xlcyIsImNyZWF0ZS11c2"
        + "VyIiwibWFuYWdlLXVzZXIiXSwiYXV0aF90aW1lIjoxNTk0MTk3MjczMDAwLCJyZWFsbSI6Ii9obWN0cyIsImV4cCI6MTU5NDIyNjA3NCwia"
        + "WF0IjoxNTk0MTk3Mjc0LCJleHBpcmVzX2luIjoyODgwMCwianRpIjoiYTJmNThmYzgtMmIwMy00M2I0LThkOTMtNmU0NWQyZTU0OTcxIn0."
        + "PTWXIvTTw54Ob1XYdP_x4l5-ITWDdbAY3-IPAPFkHDmjKgEVweabxrDIp2_RSoAcyZgza8LqJSTc00-_RzZ079nyl9pARy08BpljLZCmYdo"
        + "F2RO8CHuEVagF-SQdL37d-4pJPIMRChO0AmplBj1qMtVbuRd3WGNeUvoCtStdviFwlxvzRnLdHKwCi6AQHMaw1V9n9QyU9FxNSbwmNsCDt7"
        + "k02vLJDY9fLCsFYy5iWGCjb8lD1aX1NTv7jz2ttNNv7-smqp6L3LSSD_LCZMpf0h_3n5RXiv-N3vNpWe4ZC9u0AWQdHEE9QlKTZlsqwKSog"
        + "3yJWhyxAamdMepgW7Z8jQ";

    private static final String CLAIMS_LIST_URL = "/cases/";
    private static final String CASES_URL = "/cases/{caseId}";
    private static final String CASE_APP_URL = "/cases//{caseId}/applications";
    private static final String SUBMIT_EVENT_URL = "/cases/{caseId}/citizen/{submitterId}/event";
    private static final String ELASTICSEARCH = "{\n"
        + "\"terms\": {\n"
        + "\"reference\": [ \"1643728683977521\", \"1643642899151591\" ]\n"
        + "\n"
        + " }\n"
        + "}";

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private CaseEventService caseEventService;

    @Test
    @SneakyThrows
    public void shouldReturnHttpStatusOK() {
        SearchResult expectedCaseDetails = SearchResult.builder()
            .total(1)
            .cases(Arrays
                       .asList(CaseDetails
                                   .builder()
                                   .id(1L)
                                   .id(1L)
                                   .build()))
            .build();

        SearchResult expectedCaseData = SearchResult.builder()
            .total(1)
            .cases(Arrays.asList(CaseDetails.builder().id(1L).build()))
            .build();

        when(coreCaseDataService.searchGeneralApplication(any(), anyString()))
            .thenReturn(expectedCaseDetails);

        doPost(BEARER_TOKEN, ELASTICSEARCH, CLAIMS_LIST_URL, "")
            .andExpect(content().json(toJson(expectedCaseData)))
            .andExpect(status().isOk());

    }

    @Test
    @SneakyThrows
    public void shouldReturnHttp200() {
        CaseDetails expectedCaseDetails = CaseDetails.builder().id(1L).build();

        when(coreCaseDataService.getCase(1L, BEARER_TOKEN))
            .thenReturn(expectedCaseDetails);
        doGet(BEARER_TOKEN, CASES_URL, 1L)
            .andExpect(content().json(toJson(expectedCaseDetails)))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void shouldSubmitEventSuccessfully() {
        CaseDetails expectedCaseDetails = CaseDetails.builder().id(1L).build();
        when(caseEventService.submitEvent(any())).thenReturn(expectedCaseDetails);
        doPost(
            BEARER_TOKEN,
            EventDto.builder().event(CaseEvent.RESPOND_TO_APPLICATION).caseDataUpdate(Map.of()).build(),
            SUBMIT_EVENT_URL,
            "123",
            "123"
        ).andExpect(content().json(toJson(expectedCaseDetails)))
            .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void shouldReturnApplicationsByMainCaseId() {
        SearchResult result = SearchResult.builder().cases(List.of()).total(1).build();
        when(coreCaseDataService.searchGeneralApplication(any(), any())).thenReturn(result);
        doGet(BEARER_TOKEN, CASE_APP_URL, 1L)
                .andExpect(content().json(toJson(result)))
                .andExpect(status().isOk());
    }

}
