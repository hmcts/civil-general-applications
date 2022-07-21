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
import static uk.gov.hmcts.reform.civil.enums.CaseRole.APPLICANTSOLICITORONESPEC;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONESPEC;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWO;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWOSPEC;

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

        String caseType = caseData.getGeneralAppSuperClaimType();
        String specType = "SPEC_CLAIM";
        String unSpecType = "UNSPEC_CLAIM";

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        List<String> errors = new ArrayList<>();

        try {

            GASolicitorDetailsGAspec applicantSolicitor = caseData.getGeneralAppApplnSolicitor();

            if (caseType.equals(unSpecType)) {
                coreCaseUserService.assignCase(caseId, applicantSolicitor.getId(),
                                               applicantSolicitor.getOrganisationIdentifier(), APPLICANTSOLICITORONE
                );
            } else if (caseType.equals(specType)) {
                coreCaseUserService.assignCase(caseId, applicantSolicitor.getId(),
                                               applicantSolicitor.getOrganisationIdentifier(),
                                               APPLICANTSOLICITORONESPEC
                );
            }
            if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {
                GASolicitorDetailsGAspec respondentSolicitor1 = caseData.getGeneralAppRespondentSolicitors().get(
                        FIRST_SOLICITOR)
                    .getValue();
                if (caseType.equals(unSpecType)) {
                    coreCaseUserService
                        .assignCase(caseId, respondentSolicitor1.getId(),
                                    respondentSolicitor1.getOrganisationIdentifier(), RESPONDENTSOLICITORONE);
                } else if (caseType.equals(specType)) {
                    coreCaseUserService
                        .assignCase(caseId, respondentSolicitor1.getId(),
                                    respondentSolicitor1.getOrganisationIdentifier(), RESPONDENTSOLICITORONESPEC);
                }
                if (caseData.getGeneralAppRespondentSolicitors().size() > 1) {

                    GASolicitorDetailsGAspec respondentSolicitor2 = caseData.getGeneralAppRespondentSolicitors()
                        .get(SECOND_SOLICITOR).getValue();
                    if (caseType.equals(unSpecType)) {
                        coreCaseUserService
                            .assignCase(caseId, respondentSolicitor2.getId(),
                                        respondentSolicitor2.getOrganisationIdentifier(), RESPONDENTSOLICITORTWO);
                    } else if (caseType.equals(specType)) {
                        coreCaseUserService
                            .assignCase(caseId, respondentSolicitor2.getId(),
                                        respondentSolicitor2.getOrganisationIdentifier(), RESPONDENTSOLICITORTWOSPEC);
                    }
                }
            }
            return AboutToStartOrSubmitCallbackResponse.builder().data(caseDataBuilder.build().toMap(mapper)).errors(
                    errors)
                .build();

        } catch (Exception e) {
            log.error(e.toString());
            throw e;
        }
    }
}
