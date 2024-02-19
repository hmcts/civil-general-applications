package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_APPLICATION_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.OrgPolicyUtils.getRespondent1SolicitorOrgId;
import static uk.gov.hmcts.reform.civil.utils.OrgPolicyUtils.getRespondent2SolicitorOrgId;

@RequiredArgsConstructor
@Component
public class CreateApplicationTaskHandler implements BaseExternalTaskHandler {

    private static final String GENERAL_APPLICATION_CASE_ID = "generalApplicationCaseId";
    private static final String GENERAL_APPLICATIONS = "generalApplications";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_CLAIMANT = "claimantGaAppDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL = "respondentSolGaAppDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL_TWO = "respondentSolTwoGaAppDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_JUDGE = "gaDetailsMasterCollection";
    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;
    private final StateFlowEngine stateFlowEngine;
    private CaseData data;
    private CaseData generalAppCaseData;
    private List<Element<GeneralApplication>> generalApplications;
    private List<Element<GeneralApplicationsDetails>> judgeApplications;
    private List<Element<GeneralApplicationsDetails>> applications;
    private List<Element<GADetailsRespondentSol>> respondentSpecficGADetails;
    private List<Element<GADetailsRespondentSol>> respondentTwoSpecficGADetails;

    @Override
    public void handleTask(ExternalTask externalTask) {
        ExternalTaskInput variables = mapper.convertValue(externalTask.getAllVariables(), ExternalTaskInput.class);
        String caseId = variables.getCaseId();

        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(caseId, variables.getCaseEvent());

        CaseData caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        generalApplications = caseData.getGeneralApplications();

        judgeApplications =
            ofNullable(caseData.getGaDetailsMasterCollection()).orElse(newArrayList());

        applications =
            ofNullable(caseData.getClaimantGaAppDetails()).orElse(newArrayList());

        respondentSpecficGADetails =
            ofNullable(caseData.getRespondentSolGaAppDetails()).orElse(newArrayList());

        respondentTwoSpecficGADetails =
            ofNullable(caseData.getRespondentSolTwoGaAppDetails()).orElse(newArrayList());

        if (generalApplications != null && !generalApplications.isEmpty()) {

            var genApps = generalApplications.stream()
                .filter(application -> application.getValue() != null
                    && application.getValue().getBusinessProcess() != null
                    && application.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.STARTED
                    && application.getValue().getBusinessProcess().getProcessInstanceId() != null).findFirst();

            if (genApps.isPresent()) {

                GeneralApplication generalApplication = genApps.get().getValue();

                createGeneralApplicationCase(generalApplication);
                updateParentCaseGeneralApplication(variables, generalApplication);

                withoutNoticeNoConsent(generalApplication, caseData);

            }
        }

        data = coreCaseDataService.submitUpdate(caseId, coreCaseDataService.caseDataContentFromStartEventResponse(
            startEventResponse, getUpdatedCaseData(caseData, generalApplications,
                                                   applications,
                                                   respondentSpecficGADetails,
                                                   respondentTwoSpecficGADetails,
                                                   judgeApplications)));
    }

    private void withoutNoticeNoConsent(GeneralApplication generalApplication, CaseData caseData) {

        /*
         * Add the case to applicant solicitor collection if parent claimant is applicant
         * Hide the case if parent claimant isn't GA applicant and initiate without notice application
         * */
        if (generalApplication.getParentClaimantIsApplicant().equals(YES)) {
            applications = addApplication(
                    buildApplication(generalApplication),
                    caseData.getClaimantGaAppDetails()
            );
        }

        /*
         * Add the GA in respondent one collection if he/she initiate without notice application.
         * */

        if (generalApplication.getGeneralAppApplnSolicitor().getOrganisationIdentifier()
                .equals(getRespondent1SolicitorOrgId(caseData))) {

            GADetailsRespondentSol gaDetailsRespondentSol = buildRespApplication(generalApplication);

            if (gaDetailsRespondentSol != null) {
                respondentSpecficGADetails = addRespApplication(
                        gaDetailsRespondentSol, caseData.getRespondentSolGaAppDetails());
            }
        }

        /*
         * Add the GA in respondent two collection if he/she initiate without notice application.
         * */
        if (generalApplication.getIsMultiParty().equals(YES) && caseData.getAddApplicant2().equals(NO)
                && caseData.getRespondent2SameLegalRepresentative().equals(NO)
                && generalApplication.getGeneralAppApplnSolicitor().getOrganisationIdentifier()
                .equals(getRespondent2SolicitorOrgId(caseData))) {

            GADetailsRespondentSol gaDetailsRespondentSolTwo = buildRespApplication(
                    generalApplication);

            if (gaDetailsRespondentSolTwo != null) {
                respondentTwoSpecficGADetails = addRespApplication(
                        gaDetailsRespondentSolTwo, caseData.getRespondentSolTwoGaAppDetails());
            }
        }

    }

    private GeneralApplicationsDetails buildApplication(GeneralApplication generalApplication) {
        List<GeneralApplicationTypes> types = generalApplication.getGeneralAppType().getTypes();
        String collect = types.stream().map(GeneralApplicationTypes::getDisplayedValue)
            .collect(Collectors.joining(", "));
        return GeneralApplicationsDetails.builder()
            .generalApplicationType(collect)
            .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
            .caseLink(CaseLink.builder().caseReference(String.valueOf(
                generalAppCaseData.getCcdCaseReference())).build())
            .caseState(PENDING_APPLICATION_ISSUED.getDisplayedValue()).build();
    }

    private GADetailsRespondentSol buildRespApplication(GeneralApplication generalApplication) {
        List<GeneralApplicationTypes> types = generalApplication.getGeneralAppType().getTypes();
        String collect = types.stream().map(GeneralApplicationTypes::getDisplayedValue)
            .collect(Collectors.joining(", "));

        return GADetailsRespondentSol.builder()
            .generalApplicationType(collect)
            .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
            .caseLink(CaseLink.builder().caseReference(String.valueOf(
                generalAppCaseData.getCcdCaseReference())).build())
            .caseState(PENDING_APPLICATION_ISSUED.getDisplayedValue()).build();

    }

    private List<Element<GeneralApplicationsDetails>> addApplication(GeneralApplicationsDetails application,
                                                                     List<Element<GeneralApplicationsDetails>>
                                                                         claimantGaAppDetails) {
        List<Element<GeneralApplicationsDetails>> newApplication = ofNullable(claimantGaAppDetails)
            .orElse(newArrayList());
        newApplication.add(element(application));
        return newApplication;
    }

    private List<Element<GADetailsRespondentSol>> addRespApplication(GADetailsRespondentSol application,
                                                                     List<Element<GADetailsRespondentSol>>
                                                                         respondentSpecficGADetails) {
        List<Element<GADetailsRespondentSol>> newApplication = ofNullable(respondentSpecficGADetails)
            .orElse(newArrayList());
        newApplication.add(element(application));
        return newApplication;
    }

    private void updateParentCaseGeneralApplication(ExternalTaskInput variables,
                                                    GeneralApplication generalApplication) {
        generalApplication.getBusinessProcess().setStatus(BusinessProcessStatus.FINISHED);
        if (Objects.nonNull(generalApplication.getGeneralAppEvidenceDocument())) {
            generalApplication.getGeneralAppEvidenceDocument().clear();
        }
        generalApplication.setGeneralAppN245FormUpload(null);
        generalApplication.getBusinessProcess().setCamundaEvent(variables.getCaseEvent().name());
        if (generalAppCaseData != null && generalAppCaseData.getCcdCaseReference() != null) {
            generalApplication.addCaseLink(CaseLink.builder().caseReference(String.valueOf(
                generalAppCaseData.getCcdCaseReference())).build());
        }
    }

    private void createGeneralApplicationCase(GeneralApplication generalApplication) {
        Map<String, Object> map = generalApplication.toMap(mapper);
        map.put("isDocumentVisible", checkVisibility(generalApplication));
        map.put("generalAppNotificationDeadlineDate", generalApplication.getGeneralAppDateDeadline());

        generalAppCaseData = coreCaseDataService.createGeneralAppCase(map);
    }

    private YesOrNo checkVisibility(GeneralApplication generalApplication) {
        /*
         * Respondent Agreement is No and without notice application.
         * Application should be visible to solicitor who initiates the ga
         * */
        if ((generalApplication.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && ofNullable(generalApplication.getGeneralAppInformOtherParty()).isPresent()
            && NO.equals(generalApplication.getGeneralAppInformOtherParty().getIsWithNotice()))) {
            return NO;
        }
        /*
         * Respondent Agreement is NO and with notice.
         * Application should be visible to all solicitors
         * Consent order should be visible to all solicitors
         * */
        if ((generalApplication.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && ofNullable(generalApplication.getGeneralAppInformOtherParty()).isPresent()
            && YES.equals(generalApplication.getGeneralAppInformOtherParty().getIsWithNotice()))
            || generalApplication.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES)) {
            return YES;
        }

        /* Urgent Application */

        if (generalApplication.getGeneralAppUrgencyRequirement() != null) {
            return generalApplication.getGeneralAppUrgencyRequirement().getGeneralAppUrgency();
        }

        return YES;
    }

    @Override
    public VariableMap getVariableMap() {
        VariableMap variables = Variables.createVariables();
        var stateFlow = stateFlowEngine.evaluate(data);
        variables.putValue(FLOW_STATE, stateFlow.getState().getName());
        variables.putValue(FLOW_FLAGS, stateFlow.getFlags());
        if (generalAppCaseData != null && generalAppCaseData.getCcdCaseReference() != null) {
            variables.putValue(GENERAL_APPLICATION_CASE_ID, generalAppCaseData.getCcdCaseReference());
        }
        return variables;
    }

    private Map<String, Object> getUpdatedCaseData(CaseData caseData,
                                                   List<Element<GeneralApplication>> generalApplications,
                                                   List<Element<GeneralApplicationsDetails>>
                                                       claimantGaAppDetails,
                                                   List<Element<GADetailsRespondentSol>>
                                                       respondentSolGaAppDetails,
                                                   List<Element<GADetailsRespondentSol>>
                                                       respondentSolTwoGaAppDetails,
                                                   List<Element<GeneralApplicationsDetails>>
                                                       judgeApplications) {
        Map<String, Object> output = caseData.toMap(mapper);
        output.put(GENERAL_APPLICATIONS, generalApplications);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_CLAIMANT, claimantGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL, respondentSolGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL_TWO, respondentSolTwoGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_JUDGE, judgeApplications);
        return output;
    }
}
