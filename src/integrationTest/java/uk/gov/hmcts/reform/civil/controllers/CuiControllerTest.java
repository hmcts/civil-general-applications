package uk.gov.hmcts.reform.civil.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.citizenui.dto.EventDto;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

public class CuiControllerTest extends BaseIntegrationTest {

    private static final String SUBMIT_EVENT_URL = "/cases/{caseId}/citizen/{submitterId}/event";

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Test
    @SneakyThrows
    void shouldSubmitEventSuccessfully() {
        CaseDetails expectedCaseDetails = CaseDetails.builder().id(1L).build();
        when(coreCaseDataService.submitEventForCitizen(any(), any(), any(), any(), any())).thenReturn(expectedCaseDetails);
        doPost(
            BEARER_TOKEN,
            EventDto.builder().event(CaseEvent.CREATE_LIP_APPLICATION).generalApplicationUpdate(Map.of()).build(),
            SUBMIT_EVENT_URL,
            "123",
            "123"
        ).andExpect(content().json(toJson(expectedCaseDetails)))
            .andExpect(status().isOk());
    }

}
