package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreCaseDataService {

    private final UserService userService;
    private final CoreCaseDataApi coreCaseDataApi;
    private final SystemUpdateUserConfiguration userConfig;
    private final AuthTokenGenerator authTokenGenerator;
    private final CaseDetailsConverter caseDetailsConverter;
    private static final String RETRY_MSG = "retry with fresh token";

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
        try {
            return startUpdate(caseId, eventName, systemUpdateUser);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            systemUpdateUser = refreshSystemUpdateUser();
            return startUpdate(caseId, eventName, systemUpdateUser);
        }
    }

    private StartEventResponse startUpdate(String caseId, CaseEvent eventName, UserAuthContent systemUpdateUser) {
        return coreCaseDataApi.startEventForCaseWorker(
                systemUpdateUser.getUserToken(),
                authTokenGenerator.generate(),
                systemUpdateUser.getUserId(),
                JURISDICTION,
                CASE_TYPE,
                caseId,
                eventName.name());
    }

    public StartEventResponse startGaUpdate(String caseId, CaseEvent eventName) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();
        try {
            return startGaUpdate(caseId, eventName, systemUpdateUser);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            systemUpdateUser = refreshSystemUpdateUser();
            return startGaUpdate(caseId, eventName, systemUpdateUser);
        }
    }

    private StartEventResponse startGaUpdate(String caseId, CaseEvent eventName, UserAuthContent systemUpdateUser) {
        return coreCaseDataApi.startEventForCaseWorker(
                systemUpdateUser.getUserToken(),
                authTokenGenerator.generate(),
                systemUpdateUser.getUserId(),
                JURISDICTION,
                GENERAL_APPLICATION_CASE_TYPE,
                caseId,
                eventName.name());
    }

    public CaseData submitUpdate(String caseId, CaseDataContent caseDataContent) {
        UserAuthContent systemUpdateUser = getSystemUpdateUser();
        try {
            return submitUpdate(caseId, caseDataContent, systemUpdateUser);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            systemUpdateUser = refreshSystemUpdateUser();
            return submitUpdate(caseId, caseDataContent, systemUpdateUser);
        }
    }

    private CaseData submitUpdate(String caseId, CaseDataContent caseDataContent, UserAuthContent systemUpdateUser) {
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
        try {
            return submitGaUpdate(caseId, caseDataContent, systemUpdateUser);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            systemUpdateUser = refreshSystemUpdateUser();
            return submitGaUpdate(caseId, caseDataContent, systemUpdateUser);
        }
    }

    private CaseData submitGaUpdate(String caseId, CaseDataContent caseDataContent, UserAuthContent systemUpdateUser) {
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
        try {
            return searchCases(query, userToken);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            userToken = userService.refreshAccessToken(userConfig.getUserName(), userConfig.getPassword());
            return searchCases(query, userToken);
        }
    }

    private SearchResult searchCases(Query query, String userToken) {
        return coreCaseDataApi.searchCases(userToken, authTokenGenerator.generate(), CASE_TYPE, query.toString());
    }

    public SearchResult searchGeneralApplication(Query query) {
        String userToken = userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        try {
            return searchGeneralApplication(query, userToken);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            userToken = userService.refreshAccessToken(userConfig.getUserName(), userConfig.getPassword());
            return searchGeneralApplication(query, userToken);
        }
    }

    private SearchResult searchGeneralApplication(Query query, String userToken) {
        return coreCaseDataApi.searchCases(
                userToken,
                authTokenGenerator.generate(),
                GENERAL_APPLICATION_CASE_TYPE,
                query.toString()
        );
    }

    public CaseDetails getCase(Long caseId) {
        String userToken = userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        try {
            return getCase(caseId, userToken);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            userToken = userService.refreshAccessToken(userConfig.getUserName(), userConfig.getPassword());
            return getCase(caseId, userToken);
        }
    }

    private CaseDetails getCase(Long caseId, String userToken) {
        return coreCaseDataApi.getCase(userToken, authTokenGenerator.generate(), caseId.toString());
    }

    private UserAuthContent getSystemUpdateUser() {
        String userToken = userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        String userId = userService.getUserInfo(userToken).getUid();
        return UserAuthContent.builder().userToken(userToken).userId(userId).build();
    }

    private UserAuthContent refreshSystemUpdateUser() {
        String userToken = userService.refreshAccessToken(userConfig.getUserName(), userConfig.getPassword());
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
        try {
            return startCaseForCaseworker(eventId, systemUpdateUser);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            systemUpdateUser = refreshSystemUpdateUser();
            return startCaseForCaseworker(eventId, systemUpdateUser);
        }
    }

    private StartEventResponse startCaseForCaseworker(String eventId, UserAuthContent systemUpdateUser) {
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
        try {
            return submitForCaseWorker(caseDataContent, systemUpdateUser);
        } catch (Exception e) {
            log.info(e.getMessage());
            log.info(RETRY_MSG);
            return submitForCaseWorker(caseDataContent, systemUpdateUser);
        }
    }

    private CaseData submitForCaseWorker(CaseDataContent caseDataContent, UserAuthContent systemUpdateUser) {
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
