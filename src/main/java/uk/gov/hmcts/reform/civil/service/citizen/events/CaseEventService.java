package uk.gov.hmcts.reform.civil.service.citizen.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import static uk.gov.hmcts.reform.civil.CaseDefinitionConstants.GENERAL_APPLICATION_CASE_TYPE;
import static uk.gov.hmcts.reform.civil.CaseDefinitionConstants.JURISDICTION;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseEventService {

    private final CoreCaseDataApi coreCaseDataApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final CoreCaseDataService coreCaseDataService;

    public StartEventResponse startEvent(String authorisation, String userId, String caseId, CaseEvent event) {
        return coreCaseDataApi.startEventForCitizen(
            authorisation,
            authTokenGenerator.generate(),
            userId,
            JURISDICTION,
            GENERAL_APPLICATION_CASE_TYPE,
            caseId,
            event.name()
        );
//        return coreCaseDataApi.startEventForCaseWorker(
//                authorisation,
//                authTokenGenerator.generate(),
//                userId,
//                JURISDICTION,
//                GENERAL_APPLICATION_CASE_TYPE,
//                caseId,
//                event.name()
//        );
    }

    public CaseDetails submitEvent(EventSubmissionParams params) {
        StartEventResponse eventResponse = startEvent(
                params.getAuthorisation(),
                params.getUserId(),
                params.getCaseId(),
                params.getEvent()
        );
        CaseDataContent caseDataContent = coreCaseDataService.caseDataContentFromStartEventResponse(eventResponse, params.getUpdates());
        return coreCaseDataApi.submitEventForCitizen(
                params.getAuthorisation(),
                authTokenGenerator.generate(),
                params.getUserId(),
                JURISDICTION,
                GENERAL_APPLICATION_CASE_TYPE,
                params.getCaseId(),
                true,
                caseDataContent
        );
//        return coreCaseDataApi.submitEventForCaseWorker(
//                params.getAuthorisation(),
//                authTokenGenerator.generate(),
//                params.getUserId(),
//                JURISDICTION,
//                GENERAL_APPLICATION_CASE_TYPE,
//                params.getCaseId(),
//                true,
//                caseDataContent
//        );
    }
}
