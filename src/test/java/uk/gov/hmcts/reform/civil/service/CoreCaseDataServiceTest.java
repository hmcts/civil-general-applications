package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.SystemUpdateUserConfiguration;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.CaseDefinitionConstants.GENERAL_APPLICATION_CASE_TYPE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    CoreCaseDataService.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class CoreCaseDataServiceTest {

    private static final String USER_AUTH_TOKEN = "Bearer user-xyz";
    private static final String SERVICE_AUTH_TOKEN = "Bearer service-xyz";
    private static final String CASE_TYPE = "CIVIL";

    @MockBean
    private SystemUpdateUserConfiguration userConfig;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;
    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoreCaseDataService service;

    @BeforeEach
    void init() {
        clearInvocations(authTokenGenerator);
        clearInvocations(userService);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword()))
            .thenReturn(USER_AUTH_TOKEN);
    }

    @Nested
    class TriggerEvent {

        private static final String EVENT_ID = "INITIATE_GENERAL_APPLICATION";
        private static final String JURISDICTION = "CIVIL";
        private static final String EVENT_TOKEN = "eventToken";
        private static final String CASE_ID = "1";
        private static final String USER_ID = "User1";
        private final CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .build();
        private final CaseDetails caseDetails = CaseDetailsBuilder.builder()
            .createdDate(LocalDateTime.now())
            .data(caseData)
            .build();

        @BeforeEach
        void setUp() {
            when(userService.getUserInfo(USER_AUTH_TOKEN)).thenReturn(UserInfo.builder().uid(USER_ID).build());

            when(coreCaseDataApi.startEventForCaseWorker(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, USER_ID, JURISDICTION,
                                                         CASE_TYPE, CASE_ID, EVENT_ID
            )).thenReturn(buildStartEventResponse());

            when(coreCaseDataApi.submitEventForCaseWorker(
                eq(USER_AUTH_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION),
                eq(CASE_TYPE),
                eq(CASE_ID),
                anyBoolean(),
                any(CaseDataContent.class)
                 )
            ).thenReturn(caseDetails);
        }

        @Test
        void shouldStartAndSubmitEvent_WhenCalled() {
            service.triggerEvent(Long.valueOf(CASE_ID), CaseEvent.valueOf(EVENT_ID));

            verify(coreCaseDataApi).startEventForCaseWorker(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, USER_ID,
                                                            JURISDICTION, CASE_TYPE, CASE_ID, EVENT_ID
            );
            verify(coreCaseDataApi).submitEventForCaseWorker(
                eq(USER_AUTH_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION),
                eq(CASE_TYPE),
                eq(CASE_ID),
                anyBoolean(),
                any(CaseDataContent.class)
            );
        }

        private StartEventResponse buildStartEventResponse() {
            return StartEventResponse.builder()
                .eventId(EVENT_ID)
                .token(EVENT_TOKEN)
                .caseDetails(caseDetails)
                .build();
        }
    }

    @Nested
    class TriggerGaEvent {

        private static final String EVENT_ID = "CREATE_GENERAL_APPLICATION_CASE";
        private static final String JURISDICTION = "CIVIL";
        private static final String EVENT_TOKEN = "eventToken";
        private static final String CASE_ID = "1";
        private static final String USER_ID = "User1";
        private final CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .build();
        private final CaseDetails caseDetails = CaseDetailsBuilder.builder()
            .data(caseData)
            .build();

        @BeforeEach
        void setUp() {
            when(userService.getUserInfo(USER_AUTH_TOKEN)).thenReturn(UserInfo.builder().uid(USER_ID).build());

            when(coreCaseDataApi.startEventForCaseWorker(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, USER_ID, JURISDICTION,
                                                         GENERAL_APPLICATION_CASE_TYPE, CASE_ID, EVENT_ID
            )).thenReturn(buildStartEventResponse());

            when(coreCaseDataApi.submitEventForCaseWorker(
                     eq(USER_AUTH_TOKEN),
                     eq(SERVICE_AUTH_TOKEN),
                     eq(USER_ID),
                     eq(JURISDICTION),
                     eq(GENERAL_APPLICATION_CASE_TYPE),
                     eq(CASE_ID),
                     anyBoolean(),
                     any(CaseDataContent.class)
                 )
            ).thenReturn(caseDetails);
        }

        @Test
        void shouldStartAndSubmitEvent_WhenCalled() {
            service.triggerGaEvent(Long.valueOf(CASE_ID), CaseEvent.valueOf(EVENT_ID), new HashMap<>());

            verify(coreCaseDataApi).startEventForCaseWorker(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, USER_ID,
                                                            JURISDICTION, GENERAL_APPLICATION_CASE_TYPE,
                                                            CASE_ID, EVENT_ID
            );
            verify(coreCaseDataApi).submitEventForCaseWorker(
                eq(USER_AUTH_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION),
                eq(GENERAL_APPLICATION_CASE_TYPE),
                eq(CASE_ID),
                anyBoolean(),
                any(CaseDataContent.class)
            );
        }

        private StartEventResponse buildStartEventResponse() {
            return StartEventResponse.builder()
                .eventId(EVENT_ID)
                .token(EVENT_TOKEN)
                .caseDetails(caseDetails)
                .build();
        }
    }

    @Nested
    class CreateGeneralAppCase {

        private static final String EVENT_ID = "CREATE_GENERAL_APPLICATION_CASE";
        private static final String GENERAL_APPLICATION_CREATION = "GENERAL_APPLICATION_CREATION";
        private static final String JURISDICTION = "CIVIL";
        private static final String EVENT_TOKEN = "eventToken";
        private static final String USER_ID = "User1";
        private final CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .build();
        private final CaseDetails caseDetails = CaseDetailsBuilder.builder()
            .data(caseData)
            .build();

        @BeforeEach
        void setUp() {
            when(userService.getUserInfo(USER_AUTH_TOKEN)).thenReturn(UserInfo.builder().uid(USER_ID).build());

            when(coreCaseDataApi.startForCaseworker(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, USER_ID, JURISDICTION,
                GENERAL_APPLICATION_CASE_TYPE, GENERAL_APPLICATION_CREATION
            )).thenReturn(buildStartEventResponse());

            when(coreCaseDataApi.submitForCaseworker(
                    eq(USER_AUTH_TOKEN),
                    eq(SERVICE_AUTH_TOKEN),
                    eq(USER_ID),
                    eq(JURISDICTION),
                    eq(GENERAL_APPLICATION_CASE_TYPE),
                    anyBoolean(),
                    any(CaseDataContent.class)
                )
            ).thenReturn(caseDetails);
        }

        @Test
        void shouldStartAndSubmitEvent_WhenCalled() {

            GeneralApplication generalApplication = GeneralApplication.builder().build();
            Map<String, java.util.Map<String, Object>> supplementaryMap = new HashMap<>();
            service.createGeneralAppCase(generalApplication.toMap(objectMapper), supplementaryMap);

            verify(coreCaseDataApi).startForCaseworker(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, USER_ID,
                JURISDICTION, GENERAL_APPLICATION_CASE_TYPE, GENERAL_APPLICATION_CREATION
            );

            verify(coreCaseDataApi).submitForCaseworker(
                eq(USER_AUTH_TOKEN),
                eq(SERVICE_AUTH_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION),
                eq(GENERAL_APPLICATION_CASE_TYPE),
                anyBoolean(),
                any(CaseDataContent.class)
            );
        }

        private StartEventResponse buildStartEventResponse() {
            return StartEventResponse.builder()
                .eventId(EVENT_ID)
                .token(EVENT_TOKEN)
                .caseDetails(caseDetails)
                .build();
        }
    }

    @Nested
    class SearchCases {

        @Test
        void shouldReturnCases_WhenSearchingCasesAsSystemUpdateUser() {
            Query query = new Query(QueryBuilders.matchQuery("field", "value"), emptyList(), 0);

            List<CaseDetails> cases = List.of(CaseDetails.builder().id(1L).build());
            SearchResult searchResult = SearchResult.builder().cases(cases).build();

            when(coreCaseDataApi.searchCases(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, CASE_TYPE, query.toString()))
                .thenReturn(searchResult);

            List<CaseDetails> casesFound = service.searchCases(query).getCases();

            assertThat(casesFound).isEqualTo(cases);
            verify(coreCaseDataApi).searchCases(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, CASE_TYPE, query.toString());
            verify(userService).getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        }
    }

    @Nested
    class SearchGeneralApplications {

        @Test
        void shouldReturnGeneralApplications_WhenSearchingGeneralApplicationsAsSystemUpdateUser() {
            Query query = new Query(QueryBuilders.matchQuery("field", "value"), emptyList(), 0);

            List<CaseDetails> cases = List.of(CaseDetails.builder().id(1L).build());
            SearchResult searchResult = SearchResult.builder().cases(cases).build();

            when(coreCaseDataApi.searchCases(
                USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                GENERAL_APPLICATION_CASE_TYPE,
                query.toString()
            ))
                .thenReturn(searchResult);

            List<CaseDetails> casesFound = service.searchGeneralApplication(query).getCases();

            assertThat(casesFound).isEqualTo(cases);
            verify(coreCaseDataApi).searchCases(
                USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                GENERAL_APPLICATION_CASE_TYPE,
                query.toString()
            );
            verify(userService).getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        }
    }

    @Nested
    class GetCase {

        @Test
        void shouldReturnCase_WhenInvoked() {
            CaseDetails expectedCaseDetails = CaseDetails.builder().id(1L).build();
            when(coreCaseDataApi.getCase(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, "1"))
                .thenReturn(expectedCaseDetails);

            CaseDetails caseDetails = service.getCase(1L);

            assertThat(caseDetails).isEqualTo(expectedCaseDetails);
            verify(coreCaseDataApi).getCase(USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, "1");
            verify(userService).getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        }
    }

    @Nested
    class Retry {
        private static final String EVENT_ID = "INITIATE_GENERAL_APPLICATION";
        private static final String EXPIRED_USER_AUTH_TOKEN = "expiredToken";
        private static final String CASE_ID = "1";
        private static final String USER_ID = "User1";

        @BeforeEach
        void setUp() {
            when(userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword())).thenReturn(EXPIRED_USER_AUTH_TOKEN);
            when(userService.refreshAccessToken(userConfig.getUserName(), userConfig.getPassword())).thenReturn(USER_AUTH_TOKEN);
            when(userService.getUserInfo(anyString())).thenReturn(UserInfo.builder().uid(USER_ID).build());
            when(coreCaseDataApi.startForCaseworker(eq(EXPIRED_USER_AUTH_TOKEN), anyString(), anyString(), anyString(),
                    anyString(), anyString()
            )).thenThrow(new RuntimeException("Exception"));
            when(coreCaseDataApi.startEventForCaseWorker(eq(EXPIRED_USER_AUTH_TOKEN), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString()
            )).thenThrow(new RuntimeException("Exception"));
            when(coreCaseDataApi.submitEventForCaseWorker(eq(EXPIRED_USER_AUTH_TOKEN), anyString(), anyString(),
                    anyString(), anyString(), anyString(),
                    anyBoolean(), any(CaseDataContent.class)
            )).thenThrow(new RuntimeException("Exception"));
            when(coreCaseDataApi.submitForCaseworker(eq(EXPIRED_USER_AUTH_TOKEN), anyString(), anyString(),
                    anyString(), anyString(), anyBoolean(), any(CaseDataContent.class)
            )).thenThrow(new RuntimeException("Exception"));
            when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
            when(coreCaseDataApi.searchCases(
                    eq(EXPIRED_USER_AUTH_TOKEN),
                    anyString(),
                    anyString(),
                    anyString()
            )).thenThrow(new RuntimeException("Exception"));
            when(coreCaseDataApi.getCase(
                    eq(EXPIRED_USER_AUTH_TOKEN),
                    anyString(),
                    anyString()
            )).thenThrow(new RuntimeException("Exception"));
        }

        @Test
        void shouldRetry_startCaseForCaseworker_WhenTokenExpired() {
            service.startCaseForCaseworker(CASE_ID);
            verify(coreCaseDataApi,
                    times(2))
                    .startForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        void shouldRetry_startUpdate_WhenTokenExpired() {
            service.startUpdate(CASE_ID, CaseEvent.valueOf(EVENT_ID));
            verify(coreCaseDataApi, times(2)).startEventForCaseWorker(anyString(), anyString(), anyString(),
                    anyString(), anyString(),
                    anyString(), anyString()
            );
        }

        @Test
        void shouldRetry_startGaUpdate_WhenTokenExpired() {
            service.startGaUpdate(CASE_ID, CaseEvent.valueOf(EVENT_ID));
            verify(coreCaseDataApi, times(2)).startEventForCaseWorker(anyString(), anyString(), anyString(),
                    anyString(), anyString(),
                    anyString(), anyString()
            );
        }

        @Test
        void shouldRetry_submitUpdate_WhenTokenExpired() {
            service.submitUpdate(CASE_ID, CaseDataContent.builder().build());
            verify(coreCaseDataApi, times(2)).submitEventForCaseWorker(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(),
                    anyBoolean(), any(CaseDataContent.class)
            );
        }

        @Test
        void shouldRetry_submitGaUpdate_WhenTokenExpired() {
            service.submitGaUpdate(CASE_ID, CaseDataContent.builder().build());
            verify(coreCaseDataApi, times(2)).submitEventForCaseWorker(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(),
                    anyBoolean(), any(CaseDataContent.class)
            );
        }

        @Test
        void shouldRetry_submitForCaseWorker_WhenTokenExpired() {
            service.submitForCaseWorker(CaseDataContent.builder().build());
            verify(coreCaseDataApi,
                    times(2))
                    .submitForCaseworker(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), any(CaseDataContent.class));
        }

        @Test
        void shouldRetry_searchCases_WhenTokenExpired() {
            Query query = new Query(matchAllQuery(), List.of("reference", "other field"), 0);
            service.searchCases(query);
            verify(coreCaseDataApi, times(2)).searchCases(
                    anyString(), anyString(), anyString(), anyString()
            );
        }

        @Test
        void shouldRetry_searchGeneralApplication_WhenTokenExpired() {
            Query query = new Query(matchAllQuery(), List.of("reference", "other field"), 0);
            service.searchGeneralApplication(query);
            verify(coreCaseDataApi, times(2)).searchCases(
                    anyString(), anyString(), anyString(), anyString()
            );
        }

        @Test
        void shouldRetry_getCase_WhenTokenExpired() {
            service.getCase(1L);
            verify(coreCaseDataApi, times(2)).getCase(
                    anyString(), anyString(), anyString()
            );
        }
    }
}
