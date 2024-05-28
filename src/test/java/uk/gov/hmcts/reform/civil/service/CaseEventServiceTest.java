package uk.gov.hmcts.reform.civil.service;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.service.citizen.events.CaseEventService;
import uk.gov.hmcts.reform.civil.model.EventSubmissionParams;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    CaseEventService.class
})
public class CaseEventServiceTest {

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private CaseEventService caseEventService;

    private static final String EVENT_TOKEN = "jM4OWUxMGRkLWEyMzYt";
    private static final CaseDetails CASE_DETAILS = CaseDetails.builder()
        .id(1L)
        .data(Map.of())
        .build();
    private static final String AUTHORISATION = "authorisation";
    private static final String USER_ID = "123";
    private static final String CASE_ID = "123";
    private static final String DRAFT = "draft";
    private static final String EVENT_ID = "1";
    private static final StartEventResponse RESPONSE = StartEventResponse
        .builder()
        .eventId(EVENT_ID)
        .token(EVENT_TOKEN)
        .caseDetails(CASE_DETAILS)
        .build();

    @BeforeEach
    void setUp() {
        given(authTokenGenerator.generate()).willReturn(EVENT_TOKEN);
        given(coreCaseDataApi.startEventForCitizen(any(), any(), any(), any(), any(), any(), any()))
            .willReturn(RESPONSE);
        given(coreCaseDataApi.startForCitizen(any(), any(), any(), any(), any(), any()))
            .willReturn(RESPONSE);
        given(coreCaseDataApi.submitEventForCitizen(any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
            .willReturn(CASE_DETAILS);
        given(coreCaseDataApi.submitForCitizen(any(), any(), any(), any(), any(), anyBoolean(), any()))
            .willReturn(CASE_DETAILS);
    }

    @Test
    void shouldSubmitEventForExistingClaimSuccessfully() {
        when(coreCaseDataService.caseDataContentFromStartEventResponse(any(), anyMap())).thenCallRealMethod();

        CaseDetails caseDetails = caseEventService.submitEvent(EventSubmissionParams
                                                                   .builder()
                                                                   .updates(Maps.newHashMap())
                                                                   .event(CaseEvent.RESPOND_TO_APPLICATION)
                                                                   .caseId(CASE_ID)
                                                                   .userId(USER_ID)
                                                                   .authorisation(AUTHORISATION)
                                                                   .build());
        assertThat(caseDetails).isEqualTo(CASE_DETAILS);

        StartEventResponse startEventResponse = caseEventService
            .startEvent(AUTHORISATION, USER_ID, CASE_ID, CaseEvent.RESPOND_TO_APPLICATION);
        assertThat(startEventResponse).isEqualTo(RESPONSE);
    }

    @Test
    void shouldSubmitEventForNewApplicationSuccessfully() {
        when(coreCaseDataService.caseDataContentFromStartEventResponse(any(), anyMap())).thenCallRealMethod();

        CaseDetails caseDetails = caseEventService.submitEvent(EventSubmissionParams
                                                                   .builder()
                                                                   .updates(Maps.newHashMap())
                                                                   .event(CaseEvent.RESPOND_TO_APPLICATION)
                                                                   .caseId(DRAFT)
                                                                   .userId(USER_ID)
                                                                   .authorisation(AUTHORISATION)
                                                                   .build());
        assertThat(caseDetails).isEqualTo(CASE_DETAILS);

        StartEventResponse startEventResponse = caseEventService
            .startEvent(AUTHORISATION, USER_ID, CASE_ID, CaseEvent.RESPOND_TO_APPLICATION);
        assertThat(startEventResponse).isEqualTo(RESPONSE);
    }
}
