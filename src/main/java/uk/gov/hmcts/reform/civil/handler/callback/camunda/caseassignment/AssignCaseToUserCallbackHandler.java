package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

        try {

            GASolicitorDetailsGAspec applicantSolicitor = caseData.getGeneralAppApplnSolictor();

            coreCaseUserService.assignCase(caseId, applicantSolicitor.getId(),
                                           applicantSolicitor.getOrganisationIdentifier(), APPLICANTSOLICITORONE
            );

            GASolicitorDetailsGAspec respondentSolicitor1 = caseData.getGeneralAppRespondentSolictor().get(
                    FIRST_SOLICITOR)
                .getValue();

            coreCaseUserService
                .assignCase(caseId, respondentSolicitor1.getId(), respondentSolicitor1.getOrganisationIdentifier(),
                            RESPONDENTSOLICITORONE
                );

            if (caseData.getGeneralAppRespondentSolictor().size() > 1) {

                GASolicitorDetailsGAspec respondentSolicitor2 = caseData.getGeneralAppRespondentSolictor()
                    .get(SECOND_SOLICITOR).getValue();

                coreCaseUserService
                    .assignCase(caseId, respondentSolicitor2.getId(), respondentSolicitor2.getOrganisationIdentifier(),
                                RESPONDENTSOLICITORTWO
                    );
            }

            return AboutToStartOrSubmitCallbackResponse.builder().data(caseDataBuilder.build().toMap(mapper)).errors(
                    errors)
                .build();

        }catch (Exception e) {
            log.error(e.toString());
            throw e;
        }
    }
}
