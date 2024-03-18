package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.AssignCaseToResopondentSolHelper;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;
import uk.gov.hmcts.reform.civil.service.OrganisationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.ASSIGN_GA_ROLES;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.APPLICANTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWO;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_APPLICATION_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignCaseToUserCallbackHandler extends CallbackHandler {

    private final AssignCaseToResopondentSolHelper assignCaseToResopondentSolHelper;

    private final ObjectMapper mapper;

    private final OrganisationService organisationService;
    private final GeneralAppFeesService generalAppFeesService;
    private static final List<CaseEvent> EVENTS = List.of(ASSIGN_GA_ROLES);
    public static final String TASK_ID = "AssigningOfRoles";

    private final CoreCaseUserService coreCaseUserService;

    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::assignOrgPolicy,
            callbackKey(ABOUT_TO_SUBMIT), this::assignSolicitorCaseRole
        );
    }

    private CallbackResponse assignOrgPolicy(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        try {
            if (caseData.getCcdState().equals(PENDING_APPLICATION_ISSUED)) {
                GASolicitorDetailsGAspec applicantSolicitor = caseData.getGeneralAppApplnSolicitor();
                caseDataBuilder.applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                                                 .organisation(Organisation.builder()
                                                                                   .organisationID(applicantSolicitor.getOrganisationIdentifier())
                                                                                   .build())
                                                                 .orgPolicyCaseAssignedRole(APPLICANTSOLICITORONE.getFormattedName()).build());

            }
            List<Element<GASolicitorDetailsGAspec>>  applicantAddlSolList = caseData.getGeneralAppRespondentSolicitors().stream()
                .filter(userOrgId -> (userOrgId.getValue().getOrganisationIdentifier()
                    .equalsIgnoreCase(caseData.getGeneralAppApplnSolicitor()
                                          .getOrganisationIdentifier()))).toList();
            caseDataBuilder.generalAppApplicantAddlSolicitors(applicantAddlSolList);

            List<Element<GASolicitorDetailsGAspec>>  respondentSolicitorsList = caseData.getGeneralAppRespondentSolicitors().stream()
                .filter(userOrgId -> !(userOrgId.getValue().getOrganisationIdentifier()
                    .equalsIgnoreCase(caseData.getGeneralAppApplnSolicitor().getOrganisationIdentifier()))).toList();

            if ((!caseData.getCcdState().equals(PENDING_APPLICATION_ISSUED)
                && ((ofNullable(caseData.getGeneralAppInformOtherParty()).isPresent()
                && YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice()))
                || (caseData.getGeneralAppRespondentAgreement() != null
                && caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES))))
                || (generalAppFeesService.isFreeApplication(caseData))) {

                List<Element<GASolicitorDetailsGAspec>>  respondent2SolicitorsList = caseData.getGeneralAppRespondentSolicitors().stream()
                    .filter(userOrgId -> !(userOrgId.getValue().getOrganisationIdentifier()
                        .equalsIgnoreCase(respondentSolicitorsList.get(0).getValue().getOrganisationIdentifier()))).toList();

                caseDataBuilder.respondent1OrganisationPolicy(OrganisationPolicy.builder()
                                                                  .organisation(Organisation.builder()
                                                                                    .organisationID(respondentSolicitorsList.get(0).getValue().getOrganisationIdentifier())
                                                                                    .build())
                                                                  .orgPolicyCaseAssignedRole(RESPONDENTSOLICITORONE.getFormattedName()).build());

                if (!respondent2SolicitorsList.isEmpty()) {
                    caseDataBuilder.respondent2OrganisationPolicy(OrganisationPolicy.builder()
                                                                      .organisation(Organisation.builder()
                                                                                        .organisationID(respondent2SolicitorsList.get(0).getValue().getOrganisationIdentifier())
                                                                                        .build())
                                                                      .orgPolicyCaseAssignedRole(RESPONDENTSOLICITORTWO.getFormattedName()).build());
                }
            }
            caseDataBuilder.generalAppRespondentSolicitors(respondentSolicitorsList);
            return AboutToStartOrSubmitCallbackResponse.builder().data(caseDataBuilder.build().toMap(mapper))
                .build();

        } catch (Exception e) {
            log.error(e.toString());
            throw e;
        }
    }

    @Override
    public String camundaActivityId() {
        return TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse assignSolicitorCaseRole(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String caseId = caseData.getCcdCaseReference().toString();
        List<String> errors = new ArrayList<>();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        try {

            if (caseData.getCcdState().equals(PENDING_APPLICATION_ISSUED)) {
                GASolicitorDetailsGAspec applicantSolicitor = caseData.getGeneralAppApplnSolicitor();

                coreCaseUserService.assignCase(caseId, applicantSolicitor.getId(),
                                               applicantSolicitor.getOrganisationIdentifier(), APPLICANTSOLICITORONE
                );

                List<Element<GASolicitorDetailsGAspec>> applicantAddlSolList = caseData.getGeneralAppApplicantAddlSolicitors();
                if (applicantAddlSolList != null && !applicantAddlSolList.isEmpty()) {
                    for (Element<GASolicitorDetailsGAspec> appAddlSolElement : applicantAddlSolList) {
                        if ((applicantSolicitor.getOrganisationIdentifier() != null && applicantSolicitor.getOrganisationIdentifier()
                            .equalsIgnoreCase(appAddlSolElement.getValue().getOrganisationIdentifier()))) {
                            coreCaseUserService
                                .assignCase(caseId, appAddlSolElement.getValue().getId(),
                                            appAddlSolElement.getValue().getOrganisationIdentifier(),
                                            APPLICANTSOLICITORONE);
                        }
                    }
                }
            }

            /*
             * Don't assign the case to respondent solicitors if GA is without notice
             * Assign case to Respondent Solicitors only after the payment is made by Applicant.
             * If the Application is Free Application, then assign the respondent roles during Initiation of GA
             * */
            if ((!caseData.getCcdState().equals(PENDING_APPLICATION_ISSUED)
                && ((ofNullable(caseData.getGeneralAppInformOtherParty()).isPresent()
                && YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice()))
                || (caseData.getGeneralAppRespondentAgreement() != null
                && caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES))))
                || (generalAppFeesService.isFreeApplication(caseData))) {

                assignCaseToResopondentSolHelper.assignCaseToRespondentSolicitor(caseData, caseId);
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
