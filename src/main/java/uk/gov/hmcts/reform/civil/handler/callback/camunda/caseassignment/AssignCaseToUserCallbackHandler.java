package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.ASSIGN_GA_ROLES;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.APPLICANTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWO;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignCaseToUserCallbackHandler extends CallbackHandler {

    private final ObjectMapper mapper;
    private static final List<CaseEvent> EVENTS = List.of(ASSIGN_GA_ROLES);
    public static final String TASK_ID = "AssigningOfRoles";
    private static final int FIRST_SOLICITOR = 0;
    private static final int SECOND_SOLICITOR = 1;

    private final CoreCaseUserService coreCaseUserService;
    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::assignSolicitorCaseRole
        );
    }

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse assignSolicitorCaseRole(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());

        String caseId = caseData.getCcdCaseReference().toString();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        List<String> errors = new ArrayList<>();
        log.info("Start Case Assigning : ".concat(caseId));
        try {
            log.info("Obtaining  caseData.getGeneralAppApplnSolicitor() : ".concat(caseId));
            GASolicitorDetailsGAspec applicantSolicitor = caseData.getGeneralAppApplnSolicitor();
            log.info("applicantSolicitor value : ".concat(applicantSolicitor.toString()));
            log.info("Assigning Applicant Solicitor one : ".concat(caseId));
            coreCaseUserService.assignCase(caseId, applicantSolicitor.getId(),
                                           applicantSolicitor.getOrganisationIdentifier(), APPLICANTSOLICITORONE
            );
            log.info("Assigned Successfully : Applicant Solicitor one : ".concat(caseId));
            if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {
                log.info("Obtaining the FIRST_SOLICITOR : ".concat(caseId));
                GASolicitorDetailsGAspec respondentSolicitor1 = caseData.getGeneralAppRespondentSolicitors().get(
                        FIRST_SOLICITOR)
                    .getValue();
                log.info("respondentSolicitor1 : ".concat(respondentSolicitor1.toString()));
                log.info("Assigning Applicant RESPONDENTSOLICITORONE : ".concat(caseId));
                coreCaseUserService
                    .assignCase(caseId, respondentSolicitor1.getId(),
                                respondentSolicitor1.getOrganisationIdentifier(), RESPONDENTSOLICITORONE);
                log.info("Assigned Successfully RESPONDENTSOLICITORONE : ".concat(caseId));

                if (caseData.getGeneralAppRespondentSolicitors().size() > 1) {
                    log.info("Obtaining SECOND_SOLICITOR : ".concat(caseId));
                    GASolicitorDetailsGAspec respondentSolicitor2 = caseData.getGeneralAppRespondentSolicitors()
                        .get(SECOND_SOLICITOR).getValue();
                    log.info("Assigning Applicant SECOND_SOLICITOR : ".concat(caseId));
                    coreCaseUserService
                        .assignCase(caseId, respondentSolicitor2.getId(),
                                    respondentSolicitor2.getOrganisationIdentifier(), RESPONDENTSOLICITORTWO);
                    log.info("Assigned Successfully RESPONDENTSOLICITORTWO : ".concat(caseId));
                }
            }
            log.info("Assigning Applicant Solicitor one : ".concat(caseId));
            return AboutToStartOrSubmitCallbackResponse.builder().data(caseDataBuilder.build().toMap(mapper)).errors(
                    errors)
                .build();

        } catch (Exception e) {
            log.error(e.toString());
            throw e;
        }
    }
}
