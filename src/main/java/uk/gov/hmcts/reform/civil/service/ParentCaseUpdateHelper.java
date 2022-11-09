package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@Service
@RequiredArgsConstructor
public class ParentCaseUpdateHelper {

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper mapper;

    private static final String GENERAL_APPLICATIONS_DETAILS = "generalApplicationsDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL = "gaDetailsRespondentSol";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL_TWO = "gaDetailsRespondentSolTwo";

    public void updateParentWithGAState(CaseData generalAppCaseData, String newState) {
        String applicationId = generalAppCaseData.getCcdCaseReference().toString();
        String parentCaseId = generalAppCaseData.getGeneralAppParentCaseLink().getCaseReference();

        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(parentCaseId,
                                                                                UPDATE_CASE_WITH_GA_STATE);
        CaseData caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        List<Element<GADetailsRespondentSol>> respondentSpecficGADetails = caseData.getGaDetailsRespondentSol();

        List<Element<GADetailsRespondentSol>> respondentSpecficGADetailsTwo = caseData.getGaDetailsRespondentSolTwo();

        if (!isEmpty(respondentSpecficGADetails)) {
            /*
            * Check if the application exists in the respondentSpecficGADetails List which matches the applicationId
            * as the current application with applicationId may not present in the respondentSpecficGADetails List
            * due to requirement.
            *
            * Requirement - A Without Notice application should be hidden from any Legal Reps other than the Applicant
            *  */
            if (respondentSpecficGADetails.stream()
                .anyMatch(gaRespondentApp -> gaRespSolAppFilterCriteria(gaRespondentApp, applicationId))) {

                respondentSpecficGADetails.stream()
                    .filter(gaRespondentApp -> gaRespSolAppFilterCriteria(gaRespondentApp, applicationId))
                    .findAny().orElseThrow(IllegalArgumentException::new).getValue().setCaseState(newState);
            }
        }

        if (!isEmpty(respondentSpecficGADetailsTwo)) {
            /*
             * Check if the application exists in the respondent two List which matches the applicationId
             * as the current application with applicationId may not present in the respondentSpecficGADetailsTwo List
             * due to requirement.
             *
             * Requirement - A Without Notice application should be hidden from any Legal Reps other than the Applicant
             *  */
            if (respondentSpecficGADetailsTwo.stream()
                .anyMatch(gaRespondentApp -> gaRespSolAppFilterCriteria(gaRespondentApp, applicationId))) {

                respondentSpecficGADetailsTwo.stream()
                    .filter(gaRespondentApp -> gaRespSolAppFilterCriteria(gaRespondentApp, applicationId))
                    .findAny().orElseThrow(IllegalArgumentException::new).getValue().setCaseState(newState);
            }
        }

        List<Element<GeneralApplicationsDetails>> generalApplications = updateGaApplicationState(
            caseData,
            newState,
            applicationId
        );

        coreCaseDataService.submitUpdate(parentCaseId, coreCaseDataService.caseDataContentFromStartEventResponse(
            startEventResponse, getUpdatedCaseData(caseData, generalApplications,
                                                   respondentSpecficGADetails,
                                                   respondentSpecficGADetailsTwo)));
    }

    public void updateParentApplicationVisibilityWithNewState(CaseData generalAppCaseData, String newState) {

        String applicationId = generalAppCaseData.getCcdCaseReference().toString();
        String parentCaseId = generalAppCaseData.getGeneralAppParentCaseLink().getCaseReference();

        StartEventResponse startEventResponse = coreCaseDataService
            .startUpdate(parentCaseId, UPDATE_CASE_WITH_GA_STATE);

        CaseData caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        Optional<Element<GeneralApplicationsDetails>> generalApplicationsDetails = caseData
            .getGeneralApplicationsDetails()
            .stream().filter(application -> applicationFilterCriteria(application, applicationId)).findAny();

        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol = ofNullable(
            caseData.getGaDetailsRespondentSol()).orElse(newArrayList());

        boolean isGaDetailsRespondentSolPresent = gaDetailsRespondentSol.stream()
            .anyMatch(gaRespondentApp -> gaRespSolAppFilterCriteria(gaRespondentApp, applicationId));

        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolTwo = ofNullable(
            caseData.getGaDetailsRespondentSolTwo()).orElse(newArrayList());

        boolean isGaDetailsRespondentSolTwoPresent = gaDetailsRespondentSolTwo.stream()
            .anyMatch(gaRespondentTwoApp -> gaRespSolAppFilterCriteria(gaRespondentTwoApp, applicationId));

        if (generalApplicationsDetails.isPresent()) {
            /*
            * Respondent One Solicitor collection
            * */
            if (!isGaDetailsRespondentSolPresent) {
                gaDetailsRespondentSol.add(
                        element(
                                GADetailsRespondentSol.builder()
                                        .generalApplicationType(generalApplicationsDetails
                                                .get().getValue().getGeneralApplicationType())
                                        .generalAppSubmittedDateGAspec(generalApplicationsDetails
                                                .get().getValue()
                                                .getGeneralAppSubmittedDateGAspec())
                                        .caseLink(CaseLink.builder().caseReference(String.valueOf(
                                                generalAppCaseData.getCcdCaseReference())).build())
                                        .caseState(newState).build()));
            }

            /*
             * Respondent Two Solicitor collection
             * */
            if (!isGaDetailsRespondentSolTwoPresent) {
                gaDetailsRespondentSolTwo.add(
                    element(
                        GADetailsRespondentSol.builder()
                            .generalApplicationType(generalApplicationsDetails
                                                        .get().getValue().getGeneralApplicationType())
                            .generalAppSubmittedDateGAspec(generalApplicationsDetails
                                                               .get().getValue()
                                                               .getGeneralAppSubmittedDateGAspec())
                            .caseLink(CaseLink.builder().caseReference(String.valueOf(
                                generalAppCaseData.getCcdCaseReference())).build())
                            .caseState(newState).build()));
            }

            List<Element<GeneralApplicationsDetails>> generalApplications = updateGaApplicationState(
                caseData,
                newState,
                applicationId
            );
            CaseDataContent caseDataContent = coreCaseDataService.caseDataContentFromStartEventResponse(
                startEventResponse, getUpdatedCaseData(caseData, generalApplications,
                                                       gaDetailsRespondentSol, gaDetailsRespondentSolTwo));

            coreCaseDataService.submitUpdate(parentCaseId,  caseDataContent);
        }

    }

    private List<Element<GeneralApplicationsDetails>> updateGaApplicationState(CaseData caseData, String newState,
                                                                               String applicationId) {
        List<Element<GeneralApplicationsDetails>> generalApplications = caseData.getGeneralApplicationsDetails();
        if (!isEmpty(generalApplications)) {

            if (generalApplications.stream()
                .anyMatch(applicant -> applicationFilterCriteria(applicant, applicationId))) {

                generalApplications.stream()
                    .filter(application -> applicationFilterCriteria(application, applicationId))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new)
                    .getValue().setCaseState(newState);
            }
        }
        return generalApplications;
    }

    private boolean applicationFilterCriteria(Element<GeneralApplicationsDetails> gaDetails, String applicationId) {
        return gaDetails.getValue() != null
            && gaDetails.getValue().getCaseLink() != null
            && applicationId.equals(gaDetails.getValue().getCaseLink().getCaseReference());
    }

    private boolean gaRespSolAppFilterCriteria(Element<GADetailsRespondentSol> gaDetails, String applicationId) {
        return gaDetails.getValue() != null
            && gaDetails.getValue().getCaseLink() != null
            && applicationId.equals(gaDetails.getValue().getCaseLink().getCaseReference());
    }

    private Map<String, Object> getUpdatedCaseData(CaseData caseData,
                                                   List<Element<GeneralApplicationsDetails>> generalApplicationsDetails,
                                                   List<Element<GADetailsRespondentSol>> respondentSpecficGADetails,
                                                   List<Element<GADetailsRespondentSol>>
                                                       respondentSpecficGADetailsTwo) {
        Map<String, Object> output = caseData.toMap(mapper);
        output.put(GENERAL_APPLICATIONS_DETAILS, generalApplicationsDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL, respondentSpecficGADetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL_TWO, respondentSpecficGADetailsTwo);
        return output;
    }
}
