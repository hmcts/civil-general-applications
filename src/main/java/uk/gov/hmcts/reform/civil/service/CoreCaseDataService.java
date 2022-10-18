package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.SystemUpdateUserConfiguration;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.data.UserAuthContent;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.CaseDefinitionConstants.CASE_TYPE;
import static uk.gov.hmcts.reform.civil.CaseDefinitionConstants.GENERAL_APPLICATION_CASE_TYPE;
import static uk.gov.hmcts.reform.civil.CaseDefinitionConstants.JURISDICTION;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERAL_APPLICATION_CREATION;
import static uk.gov.hmcts.reform.civil.handler.tasks.BaseExternalTaskHandler.log;

@Service
@RequiredArgsConstructor
public class CoreCaseDataService {

    private final UserService userService;
    private final CoreCaseDataApi coreCaseDataApi;
    private final SystemUpdateUserConfiguration userConfig;
    private final AuthTokenGenerator authTokenGenerator;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;

    public void triggerEvent(Long caseId, CaseEvent eventName) {
        triggerEvent(caseId, eventName, Map.of());
    }

    public void triggerEvent(Long caseId, CaseEvent eventName, Map<String, Object> contentModified) {
        StartEventResponse startEventResponse = startUpdate(caseId.toString(), eventName);
        submitUpdate(caseId.toString(), caseDataContentFromStartEventResponse(startEventResponse, contentModified));
    }

    public CaseData createGeneralAppCase(Map<String, Object> caseDataMap) {
        var startEventResponse = startCaseForCaseworker(GENERAL_APPLICATION_CREATION.name());
        return submitForCaseWorker(caseDataContent(startEventResponse, caseDataMap));
    }

    public StartEventResponse startUpdate(String caseId, CaseEvent eventName) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();

        return coreCaseDataApi.startEventForCaseWorker(
            systemUpdateUser.getUserToken(),
            authTokenGenerator.generate(),
            systemUpdateUser.getUserId(),
            JURISDICTION,
            CASE_TYPE,
            caseId,
            eventName.name()
        );
    }

    public StartEventResponse startGaUpdate(String caseId, CaseEvent eventName) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();

        return coreCaseDataApi.startEventForCaseWorker(
            systemUpdateUser.getUserToken(),
            authTokenGenerator.generate(),
            systemUpdateUser.getUserId(),
            JURISDICTION,
            GENERAL_APPLICATION_CASE_TYPE,
            caseId,
            eventName.name()
        );
    }

    @java.lang.SuppressWarnings({"java:S2629", "java:S3457", "java:S2139", "java:S112"})
    public CaseData submitUpdate(String caseId, CaseDataContent caseDataContent) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();
        try {
            log.info("submitUpdate- caseDataContent before calling coreCaseDataApi.submitEventForCaseWorker : "
                         + mapper.writeValueAsString(caseDataContent));
        } catch (JsonProcessingException e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        }
        CaseDetails caseDetails = coreCaseDataApi.submitEventForCaseWorker(
            systemUpdateUser.getUserToken(),
            authTokenGenerator.generate(),
            systemUpdateUser.getUserId(),
            JURISDICTION,
            CASE_TYPE,
            caseId,
            true,
            caseDataContent
        );
        return caseDetailsConverter.toCaseData(caseDetails);
    }

    public CaseData submitGaUpdate(String caseId, CaseDataContent caseDataContent) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();

        CaseDetails caseDetails = coreCaseDataApi.submitEventForCaseWorker(
            systemUpdateUser.getUserToken(),
            authTokenGenerator.generate(),
            systemUpdateUser.getUserId(),
            JURISDICTION,
            GENERAL_APPLICATION_CASE_TYPE,
            caseId,
            true,
            caseDataContent
        );
        return caseDetailsConverter.toCaseData(caseDetails);
    }

    public void triggerGaEvent(Long caseId, CaseEvent eventName, Map<String, Object> contentModified) {
        StartEventResponse startEventResponse = startGaUpdate(caseId.toString(), eventName);
        submitGaUpdate(caseId.toString(), caseDataContentFromStartEventResponse(startEventResponse, contentModified));
    }

    public SearchResult searchCases(Query query) {
        String userToken = userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        return coreCaseDataApi.searchCases(userToken, authTokenGenerator.generate(), CASE_TYPE, query.toString());
    }

    public SearchResult searchGeneralApplication(Query query) {
        String userToken = userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        return coreCaseDataApi.searchCases(
            userToken,
            authTokenGenerator.generate(),
            GENERAL_APPLICATION_CASE_TYPE,
            query.toString()
        );
    }

    public CaseDetails getCase(Long caseId) {
        String userToken = userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        return coreCaseDataApi.getCase(userToken, authTokenGenerator.generate(), caseId.toString());
    }

    private UserAuthContent getSystemUpdateUser() {
        String userToken = userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        String userId = userService.getUserInfo(userToken).getUid();
        return UserAuthContent.builder().userToken(userToken).userId(userId).build();
    }

    public CaseDataContent caseDataContentFromStartEventResponse(
        StartEventResponse startEventResponse, Map<String, Object> contentModified) {
        var payload = new HashMap<>(startEventResponse.getCaseDetails().getData());
        payload.putAll(contentModified);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder()
                       .id(startEventResponse.getEventId())
                       .build())
            .data(payload)
            .build();
    }

    public StartEventResponse startCaseForCaseworker(String eventId) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();
        return coreCaseDataApi.startForCaseworker(
            systemUpdateUser.getUserToken(),
            authTokenGenerator.generate(),
            systemUpdateUser.getUserId(),
            JURISDICTION,
            GENERAL_APPLICATION_CASE_TYPE,
            eventId);
    }

    public CaseData submitForCaseWorker(CaseDataContent caseDataContent) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();
        CaseDetails caseDetails = coreCaseDataApi.submitForCaseworker(
            systemUpdateUser.getUserToken(),
            authTokenGenerator.generate(),
            systemUpdateUser.getUserId(),
            JURISDICTION,
            GENERAL_APPLICATION_CASE_TYPE,
            true,
            caseDataContent
        );
        return caseDetailsConverter.toCaseData(caseDetails);
    }

    private CaseDataContent caseDataContent(StartEventResponse startEventResponse, Map<String, Object> caseDataMap) {
        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        data.putAll(caseDataMap);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .data(data)
            .build();
    }
}
