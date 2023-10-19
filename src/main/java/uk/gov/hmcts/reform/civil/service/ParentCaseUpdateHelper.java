package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseManagementCategory;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDateGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAStatementOfTruth;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_APPLICATION_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_APPLICATION_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.handler.tasks.BaseExternalTaskHandler.log;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@Service
@RequiredArgsConstructor
public class ParentCaseUpdateHelper {

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper mapper;

    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_CLAIMANT = "claimantGaAppDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL = "respondentSolGaAppDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL_TWO = "respondentSolTwoGaAppDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_JUDGE = "gaDetailsMasterCollection";
    private static final String GA_DRAFT_FORM = "gaDraft";
    private static final String[] DOCUMENT_TYPES = {
        "generalOrder", "dismissalOrder",
        "directionOrder", "hearingNotice",
        "gaResp", GA_DRAFT_FORM
    };
    private static final String CLAIMANT_ROLE = "Claimant";
    private static final String RESPONDENTSOL_ROLE = "RespondentSol";
    private static final String RESPONDENTSOL_TWO_ROLE = "RespondentSolTwo";
    private String[] roles = {CLAIMANT_ROLE, RESPONDENTSOL_ROLE, RESPONDENTSOL_TWO_ROLE};
    private static final String GA_EVIDENCE = "gaEvidence";
    private static final String CIVIL_GA_EVIDENCE = "generalAppEvidence";
    private static final String FREE_KEYWORD = "FREE";

    protected static List<CaseState> DOCUMENT_STATES = Arrays.asList(
            AWAITING_ADDITIONAL_INFORMATION,
            AWAITING_WRITTEN_REPRESENTATIONS,
            AWAITING_DIRECTIONS_ORDER_DOCS,
            PENDING_APPLICATION_ISSUED,
            APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION
    );

    public void updateParentWithGAState(CaseData generalAppCaseData, String newState) {

        String applicationId = generalAppCaseData.getCcdCaseReference().toString();
        String parentCaseId = generalAppCaseData.getGeneralAppParentCaseLink().getCaseReference();
        String[] docVisibilityRoles = new String[4];

        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(parentCaseId,
                                                                                UPDATE_CASE_WITH_GA_STATE);
        CaseData caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        List<Element<GADetailsRespondentSol>> respondentSpecficGADetails =
            ofNullable(caseData.getRespondentSolGaAppDetails()).orElse(newArrayList());

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
                docVisibilityRoles[0] = RESPONDENTSOL_ROLE;
            }
        }

        List<Element<GADetailsRespondentSol>> respondentSpecficGADetailsTwo =
            ofNullable(caseData.getRespondentSolTwoGaAppDetails()).orElse(newArrayList());

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
                docVisibilityRoles[1] = RESPONDENTSOL_TWO_ROLE;
            }
        }

        /*
         * Check if the application exists in the main claim claimant List which matches the applicationId
         * as the current application with applicationId may not present in the Claimant List
         * due to requirement.
         *
         * Requirement - A Without Notice application should be hidden from any Legal Reps other than the Applicant
         * e.g Main claim defendant initiate the GA without notice which should be hidden to main claim claimant
         * unless judge uncloak it
         *  */
        List<Element<GeneralApplicationsDetails>> generalApplications = updateGaApplicationState(
            caseData,
            newState,
            applicationId,
            docVisibilityRoles
        );

        /*
         * Check if the application exists in the Judge List which matches the applicationId
         *  */
        List<Element<GeneralApplicationsDetails>> gaDetailsMasterCollection = updateJudgeGaApplicationState(
            caseData,
            newState,
            applicationId
        );
        docVisibilityRoles[3] = "Staff";

        List<Element<GeneralApplication>> civilGeneralApplications = caseData.getGeneralApplications();

        if (generalAppCaseData.getCcdState().equals(PENDING_APPLICATION_ISSUED)) {
            if (!isEmpty(civilGeneralApplications)) {

                List<Element<GeneralApplication>> generalApplicationsList = civilGeneralApplications.stream()
                    .filter(app -> !app.getValue().getCaseLink().getCaseReference().equals(applicationId))
                    .toList();

                GeneralApplication generalApplication = civilGeneralApplications.stream()
                    .filter(app -> app.getValue().getCaseLink().getCaseReference().equals(applicationId))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new)
                    .getValue();

                civilGeneralApplications =
                    addApplication(buildGeneralApplication(generalApplication), generalApplicationsList);

            }
        }

        Map<String, Object> updateMap = getUpdatedCaseData(caseData, civilGeneralApplications, generalApplications,
                respondentSpecficGADetails,
                respondentSpecficGADetailsTwo,
                gaDetailsMasterCollection);
        if (DOCUMENT_STATES.contains(generalAppCaseData.getCcdState())) {
            updateCaseDocument(updateMap, caseData, generalAppCaseData, docVisibilityRoles);
        }
        if ((Objects.nonNull(generalAppCaseData.getGeneralAppEvidenceDocument())
            && !generalAppCaseData.getGeneralAppEvidenceDocument().isEmpty())
            || (Objects.nonNull(generalAppCaseData.getGaDraftDocument())
            && !generalAppCaseData.getGaDraftDocument().isEmpty())) {
            updateEvidence(updateMap, caseData, generalAppCaseData, docVisibilityRoles);
        }
        coreCaseDataService.submitUpdate(parentCaseId, coreCaseDataService.caseDataContentFromStartEventResponse(
            startEventResponse, updateMap));
    }

    protected void updateEvidence(Map<String, Object> updateMap, CaseData civilCaseData,
                                  CaseData generalAppCaseData, String[] docVisibilityRoles) {
        String[] evidenceRole = null;
        if (generalAppCaseData.getCcdState().equals(PENDING_APPLICATION_ISSUED)) {
            String[] evidenceRoleBefore = new String[1];
            evidenceRoleBefore[0] = findGaCreator(civilCaseData, generalAppCaseData);
            evidenceRole = evidenceRoleBefore;
        } else if (generalAppCaseData.getCcdState().equals(AWAITING_APPLICATION_PAYMENT)) {
            evidenceRole = docVisibilityRoles;
        }
        if (Objects.nonNull(evidenceRole)) {
            updateSingleTypeByRoles(updateMap, GA_EVIDENCE, evidenceRole,
                    civilCaseData, generalAppCaseData);
        }
    }

    protected void updateSingleTypeByRoles(Map<String, Object> updateMap, String type, String[] roles,
                                         CaseData civilCaseData, CaseData generalAppCaseData) {
        for (String role : roles) {
            try {
                updateCaseDocumentByType(updateMap, type, role, civilCaseData, generalAppCaseData);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    protected String findGaCreator(CaseData civilCaseData, CaseData generalAppCaseData) {
        if (generalAppCaseData.getParentClaimantIsApplicant().equals(YES)) {
            return CLAIMANT_ROLE;
        }
        String creatorId = generalAppCaseData.getGeneralAppApplnSolicitor().getOrganisationIdentifier();
        String respondent1OrganisationId = civilCaseData.getRespondent1OrganisationPolicy().getOrganisation()
                != null ? civilCaseData.getRespondent1OrganisationPolicy().getOrganisation()
                .getOrganisationID() : civilCaseData.getRespondent1OrganisationIDCopy();
        if (creatorId
                .equals(respondent1OrganisationId)) {
            return RESPONDENTSOL_ROLE;
        }
        String respondent2OrganisationId = civilCaseData.getRespondent2OrganisationPolicy().getOrganisation()
                != null ? civilCaseData.getRespondent2OrganisationPolicy().getOrganisation()
                .getOrganisationID() : civilCaseData.getRespondent2OrganisationIDCopy();
        if (generalAppCaseData.getIsMultiParty().equals(YES) && civilCaseData.getAddApplicant2().equals(NO)
                && civilCaseData.getRespondent2SameLegalRepresentative().equals(NO)
                && creatorId
                .equals(respondent2OrganisationId)) {
            return RESPONDENTSOL_TWO_ROLE;
        }
        return null;
    }

    public void updateJudgeAndRespondentCollectionAfterPayment(CaseData generalAppCaseData) {

        String applicationId = generalAppCaseData.getCcdCaseReference().toString();
        String parentCaseId = generalAppCaseData.getGeneralAppParentCaseLink().getCaseReference();
        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(parentCaseId,
                                                                                UPDATE_CASE_WITH_GA_STATE);
        CaseData parentCaseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        List<Element<GeneralApplicationsDetails>> gaMasterDetails = ofNullable(
            parentCaseData.getGaDetailsMasterCollection()).orElse(newArrayList());

        List<Element<GeneralApplicationsDetails>> gaClaimantDetails = ofNullable(
            parentCaseData.getClaimantGaAppDetails()).orElse(newArrayList());

        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol = ofNullable(
            parentCaseData.getRespondentSolGaAppDetails()).orElse(newArrayList());
        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol2 = ofNullable(
            parentCaseData.getRespondentSolTwoGaAppDetails()).orElse(newArrayList());

        if (generalAppCaseData.getParentClaimantIsApplicant().equals(YES)) {
            Optional<Element<GeneralApplicationsDetails>> claimantCollection = gaClaimantDetails
                .stream().filter(claimantApp -> applicationFilterCriteria(claimantApp, applicationId)).findAny();
            claimantCollection.ifPresent(generalApplicationsDetailsElement -> gaMasterDetails.add(
                element(
                    GeneralApplicationsDetails.builder()
                        .generalApplicationType(generalApplicationsDetailsElement.getValue().getGeneralApplicationType())
                        .generalAppSubmittedDateGAspec(generalApplicationsDetailsElement.getValue()
                                                           .getGeneralAppSubmittedDateGAspec())
                        .caseLink(CaseLink.builder().caseReference(String.valueOf(
                            generalAppCaseData.getCcdCaseReference())).build()).build())));
        } else if (generalAppCaseData.getIsMultiParty().equals(NO)) {
            updateJudgeOrClaimantFromRespCollection(generalAppCaseData, applicationId, gaMasterDetails, gaDetailsRespondentSol);
        } else {
            updateJudgeOrClaimantFromRespCollection(generalAppCaseData, applicationId, gaMasterDetails, gaDetailsRespondentSol2);
        }
        /**
         * Respondent Agreement is NO and with notice.
         * Application should be visible to all solicitor
         * Consent order should be visible to all solicitors
         */
        if ((generalAppCaseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && ofNullable(generalAppCaseData.getGeneralAppInformOtherParty()).isPresent()
            && YES.equals(generalAppCaseData.getGeneralAppInformOtherParty().getIsWithNotice()))
            || generalAppCaseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES)) {

            if (generalAppCaseData.getParentClaimantIsApplicant().equals(YES)) {
                updateRespCollectionFromClaimant(generalAppCaseData, applicationId, gaDetailsRespondentSol, gaClaimantDetails);
                if (generalAppCaseData.getIsMultiParty().equals(YES)) {
                    updateRespCollectionFromClaimant(generalAppCaseData, applicationId, gaDetailsRespondentSol2, gaClaimantDetails);
                }
            } else {
                if (generalAppCaseData.getIsMultiParty().equals(NO)) {
                    updateJudgeOrClaimantFromRespCollection(generalAppCaseData, applicationId, gaClaimantDetails, gaDetailsRespondentSol);
                } else if (generalAppCaseData.getIsMultiParty().equals(YES) && gaDetailsRespondentSol.isEmpty()) {
                    updateJudgeOrClaimantFromRespCollection(generalAppCaseData, applicationId, gaClaimantDetails, gaDetailsRespondentSol2);
                    updateRespCollectionForMultiParty(generalAppCaseData, applicationId, gaDetailsRespondentSol, gaDetailsRespondentSol2);
                } else {
                    updateJudgeOrClaimantFromRespCollection(generalAppCaseData, applicationId, gaClaimantDetails, gaDetailsRespondentSol);
                    updateRespCollectionForMultiParty(generalAppCaseData, applicationId, gaDetailsRespondentSol2, gaDetailsRespondentSol);
                }
            }

        }

        Map<String, Object> updateMap = getUpdatedCaseData(parentCaseData, parentCaseData.getGeneralApplications(),
                                                           gaClaimantDetails,
                                                           gaDetailsRespondentSol,
                                                           gaDetailsRespondentSol2,
                                                           gaMasterDetails);

        CaseDataContent caseDataContent = coreCaseDataService.caseDataContentFromStartEventResponse(
            startEventResponse, updateMap);

        coreCaseDataService.submitUpdate(parentCaseId, caseDataContent);
    }

    private void updateRespCollectionForMultiParty(CaseData generalAppCaseData, String applicationId,
                                                  List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol,
                                                  List<Element<GADetailsRespondentSol>> gaRespondentSol) {
        Optional<Element<GADetailsRespondentSol>> respCollection = gaRespondentSol
            .stream().filter(respCollectionApp -> applicationRespFilterCriteria(respCollectionApp, applicationId)).findAny();
        respCollection.ifPresent(generalApplicationsDetailsElement -> gaDetailsRespondentSol.add(
            element(
                GADetailsRespondentSol.builder()
                    .generalApplicationType(generalApplicationsDetailsElement.getValue().getGeneralApplicationType())
                    .generalAppSubmittedDateGAspec(generalApplicationsDetailsElement.getValue()
                                                       .getGeneralAppSubmittedDateGAspec())
                    .caseLink(CaseLink.builder().caseReference(String.valueOf(
                        generalAppCaseData.getCcdCaseReference())).build()).build())));

    }

    private void updateRespCollectionFromClaimant(CaseData generalAppCaseData, String applicationId,
                                                  List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol,
                                                  List<Element<GeneralApplicationsDetails>> gaClaimantDetails) {

        Optional<Element<GeneralApplicationsDetails>> claimantCollection = gaClaimantDetails
            .stream().filter(claimantApp -> applicationFilterCriteria(claimantApp, applicationId)).findAny();
        claimantCollection.ifPresent(generalApplicationsDetailsElement -> gaDetailsRespondentSol.add(
            element(
                GADetailsRespondentSol.builder()
                    .generalApplicationType(generalApplicationsDetailsElement.getValue().getGeneralApplicationType())
                    .generalAppSubmittedDateGAspec(generalApplicationsDetailsElement.getValue()
                                                       .getGeneralAppSubmittedDateGAspec())
                    .caseLink(CaseLink.builder().caseReference(String.valueOf(
                        generalAppCaseData.getCcdCaseReference())).build()).build())));

    }

    private void updateJudgeOrClaimantFromRespCollection(CaseData generalAppCaseData, String applicationId,
                                                         List<Element<GeneralApplicationsDetails>> gaMasterDetails,
                                                         List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol) {
        if (!gaDetailsRespondentSol.isEmpty()) {
            Optional<Element<GADetailsRespondentSol>> respondentSolCollection = gaDetailsRespondentSol
                .stream().filter(respondentSolElement2 -> gaRespSolAppFilterCriteria(respondentSolElement2, applicationId)).findAny();
            respondentSolCollection.ifPresent(respondentSolElement -> gaMasterDetails.add(
                element(
                    GeneralApplicationsDetails.builder()
                        .generalApplicationType(respondentSolElement.getValue().getGeneralApplicationType())
                        .generalAppSubmittedDateGAspec(respondentSolElement.getValue()
                                                           .getGeneralAppSubmittedDateGAspec())
                        .caseLink(CaseLink.builder().caseReference(String.valueOf(
                            generalAppCaseData.getCcdCaseReference())).build()).build())));
        }
    }

    public void updateParentApplicationVisibilityWithNewState(CaseData generalAppCaseData, String newState) {

        String applicationId = generalAppCaseData.getCcdCaseReference().toString();
        String parentCaseId = generalAppCaseData.getGeneralAppParentCaseLink().getCaseReference();

        StartEventResponse startEventResponse = coreCaseDataService
            .startUpdate(parentCaseId, UPDATE_CASE_WITH_GA_STATE);

        CaseData caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        /*
        * check if the applicant exits in master collection Judge
        * */
        Optional<Element<GeneralApplicationsDetails>> generalApplicationsDetails = caseData
            .getGaDetailsMasterCollection()
            .stream().filter(application -> applicationFilterCriteria(application, applicationId)).findAny();

        if (generalApplicationsDetails.isPresent()) {

            /*
            * Respondent One Solicitor collection
            * */
            List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol = ofNullable(
                caseData.getRespondentSolGaAppDetails()).orElse(newArrayList());

            boolean isGaDetailsRespondentSolPresent = gaDetailsRespondentSol.stream()
                .anyMatch(gaRespondentApp -> gaRespSolAppFilterCriteria(gaRespondentApp, applicationId));

            /*
            * Add the GA into Respondent one solicitor collection
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
            } else {
                /*
                * Update the ga with new state in respondent one solicitor collection
                * */
                gaDetailsRespondentSol = updateGaDetailsRespondentOne(caseData, newState, applicationId);
            }

            /*
             * Respondent Two Solicitor collection
             * */
            List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolTwo = ofNullable(
                caseData.getRespondentSolTwoGaAppDetails()).orElse(newArrayList());

            boolean isGaDetailsRespondentSolTwoPresent = gaDetailsRespondentSolTwo.stream()
                .anyMatch(gaRespondentTwoApp -> gaRespSolAppFilterCriteria(gaRespondentTwoApp, applicationId));

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
            } else {
                /*
                 * Update the ga with new state in respondent one solicitor collection
                 * */
                gaDetailsRespondentSolTwo = updateGaDetailsRespondentTwo(caseData, newState, applicationId);
            }

            /*
             * Claimant Solicitor collection
             * */
            List<Element<GeneralApplicationsDetails>> gaDetailsClaimant = ofNullable(
                caseData.getClaimantGaAppDetails()).orElse(newArrayList());

            boolean isGaDetailsClaimantPresent = gaDetailsClaimant.stream()
                .anyMatch(gaClaimant -> applicationFilterCriteria(gaClaimant, applicationId));

            if (!isGaDetailsClaimantPresent) {
                gaDetailsClaimant.add(
                    element(
                        GeneralApplicationsDetails.builder()
                            .generalApplicationType(generalApplicationsDetails
                                                        .get().getValue().getGeneralApplicationType())
                            .generalAppSubmittedDateGAspec(generalApplicationsDetails
                                                               .get().getValue()
                                                               .getGeneralAppSubmittedDateGAspec())
                            .caseLink(CaseLink.builder().caseReference(String.valueOf(
                                generalAppCaseData.getCcdCaseReference())).build())
                            .caseState(newState).build()));
            } else {
                /*
                 * Update the ga with new state in respondent one solicitor collection
                 * */
                gaDetailsClaimant = updateGaApplicationState(
                    caseData,
                    newState,
                    applicationId,
                    null
                );
            }

            /*
            * Judge Collection
            * */
            List<Element<GeneralApplicationsDetails>> gaDetailsMasterCollection = updateJudgeGaApplicationState(
                caseData,
                newState,
                applicationId
            );
            Map<String, Object> updateMap = getUpdatedCaseData(caseData, caseData.getGeneralApplications(),
                                                               gaDetailsClaimant,
                                                               gaDetailsRespondentSol,
                                                               gaDetailsRespondentSolTwo,
                                                               gaDetailsMasterCollection);
            /*
             * update documents
             * */
            if (gaDetailsRespondentSolTwo.isEmpty()) {
                roles[2] = null;
            }
            updateCaseDocument(updateMap, caseData, generalAppCaseData, roles);
            CaseDataContent caseDataContent = coreCaseDataService.caseDataContentFromStartEventResponse(
                startEventResponse, updateMap);

            coreCaseDataService.submitUpdate(parentCaseId, caseDataContent);
        }

    }

    protected void updateCaseDocument(Map<String, Object> updateMap,
                                    CaseData civilCaseData, CaseData generalAppCaseData, String[] roles) {
        for (String role : roles) {
            if (Objects.nonNull(role)) {
                updateCaseDocumentByRole(updateMap, role,
                        civilCaseData, generalAppCaseData);
            }
        }
    }

    protected void updateCaseDocumentByRole(Map<String, Object> updateMap, String role,
                                          CaseData civilCaseData, CaseData generalAppCaseData) {
        for (String type : DOCUMENT_TYPES) {
            try {
                updateCaseDocumentByType(updateMap, type, role, civilCaseData, generalAppCaseData);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Update GA document collection at civil case.
     *
     * @param updateMap      output map for update civil case.
     * @param civilCaseData civil case data.
     * @param generalAppCaseData    GA case data.
     * @param type document type.
     *             when get from GA data, add 'get' to the name then call getter to access related GA document field.
     *             when get from Civil data, add 'get' with role name then call getter
     * @param role role name. to be added with type to make the ga getter
     *
     */
    @SuppressWarnings("unchecked")
    protected void updateCaseDocumentByType(Map<String, Object> updateMap, String type, String role,
                                    CaseData civilCaseData, CaseData generalAppCaseData) throws Exception {
        if (Objects.isNull(role)) {
            return;
        }
        String gaCollectionName = type + "Document";
        if (type.equals(GA_EVIDENCE)) {
            gaCollectionName = CIVIL_GA_EVIDENCE + "Document";
        }

        String civilCollectionName = type + "Doc" + role;
        Method gaGetter = ReflectionUtils.findMethod(CaseData.class,
                "get" + StringUtils.capitalize(gaCollectionName));
        List<Element<?>> gaDocs =
                (List<Element<?>>) (gaGetter != null ? gaGetter.invoke(generalAppCaseData) : null);
        Method civilGetter = ReflectionUtils.findMethod(CaseData.class,
                "get" + StringUtils.capitalize(civilCollectionName));
        List<Element<?>> civilDocs =
                (List<Element<?>>) ofNullable(civilGetter != null ? civilGetter.invoke(civilCaseData) : null)
                        .orElse(newArrayList());
        if (gaDocs != null && !(type.equals(GA_DRAFT_FORM))) {
            List<UUID> ids = civilDocs.stream().map(Element::getId).toList();
            for (Element<?> gaDoc : gaDocs) {
                if (!ids.contains(gaDoc.getId())) {
                    civilDocs.add(gaDoc);
                }
            }
        } else if (gaDocs != null && gaDocs.size() == 1
            && checkIfDocumentExists(civilDocs, gaDocs) < 1) {
            civilDocs.addAll(gaDocs);
        }
        updateMap.put(civilCollectionName, civilDocs.isEmpty() ? null : civilDocs);
    }

    @SuppressWarnings("unchecked")
    protected int checkIfDocumentExists(List<Element<?>> civilCaseDocumentList,
                                        List<Element<?>> gaCaseDocumentlist) {

        if (gaCaseDocumentlist.get(0).getValue().getClass().equals(CaseDocument.class)) {
            List<Element<CaseDocument>> civilCaseList = civilCaseDocumentList.stream()
                .map(element -> (Element<CaseDocument>) element)
                .toList();
            List<Element<CaseDocument>> gaCaseList = gaCaseDocumentlist.stream()
                .map(element -> (Element<CaseDocument>) element)
                .toList();

            return civilCaseList.stream().filter(civilDocument -> gaCaseList
               .parallelStream().anyMatch(gaDocument -> gaDocument.getValue().getDocumentLink()
                   .equals(civilDocument.getValue().getDocumentLink()))).toList().size();
        } else {
            List<Element<Document>> civilCaseList = civilCaseDocumentList.stream()
                   .map(element -> (Element<Document>) element)
                   .toList();

            List<Element<Document>> gaCaseList = gaCaseDocumentlist.stream()
                   .map(element -> (Element<Document>) element)
                   .toList();

            return civilCaseList.stream().filter(civilDocument -> gaCaseList
                   .parallelStream().anyMatch(gaDocument -> gaDocument.getValue().getDocumentUrl()
                       .equals(civilDocument.getValue().getDocumentUrl()))).toList().size();
        }
    }

    private List<Element<GeneralApplicationsDetails>> updateGaApplicationState(CaseData caseData, String newState,
                                                                               String applicationId, String[] roles) {
        List<Element<GeneralApplicationsDetails>> generalApplications = ofNullable(
            caseData.getClaimantGaAppDetails()).orElse(newArrayList());

        if (!isEmpty(generalApplications)
            && generalApplications.stream()
                .anyMatch(applicant -> applicationFilterCriteria(applicant, applicationId))) {

            generalApplications.stream()
                    .filter(application -> applicationFilterCriteria(application, applicationId))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new)
                    .getValue().setCaseState(newState);
            if (Objects.nonNull(roles)) {
                roles[2] = CLAIMANT_ROLE;
            }
        }
        return generalApplications;
    }

    private List<Element<GeneralApplicationsDetails>> updateJudgeGaApplicationState(CaseData caseData, String newState,
                                                                               String applicationId) {
        List<Element<GeneralApplicationsDetails>> generalApplications = caseData.getGaDetailsMasterCollection();
        if (!isEmpty(generalApplications)
            && generalApplications.stream()
                .anyMatch(applicant -> applicationFilterCriteria(applicant, applicationId))) {

            generalApplications.stream()
                    .filter(application -> applicationFilterCriteria(application, applicationId))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new)
                    .getValue().setCaseState(newState);
        }
        return generalApplications;
    }

    private GeneralApplication buildGeneralApplication(GeneralApplication generalApplication) {
        GeneralApplication.GeneralApplicationBuilder applicationBuilder = generalApplication.toBuilder();

        applicationBuilder.generalAppType(GAApplicationType.builder().build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().build())
            .generalAppPBADetails(GAPbaDetails.builder().build())
            .generalAppDetailsOfOrder(EMPTY)
            .generalAppReasonsOfOrder(EMPTY)
            .generalAppInformOtherParty(GAInformOtherParty.builder().build())
            .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().build())
            .generalAppStatementOfTruth(GAStatementOfTruth.builder().build())
            .generalAppHearingDate(GAHearingDateGAspec.builder().build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().build())
            .generalAppHearingDetails(GAHearingDetails.builder().build())
            .gaApplicantDisplayName(EMPTY)
            .civilServiceUserRoles(IdamUserDetails.builder().build())
            .generalAppRespondentSolicitors(Collections.emptyList())
            .generalAppEvidenceDocument(Collections.emptyList())
            .applicantPartyName(EMPTY)
            .claimant1PartyName(EMPTY)
            .claimant2PartyName(EMPTY)
            .defendant1PartyName(EMPTY)
            .isMultiParty(null)
            .parentClaimantIsApplicant(null)
            .isCtscLocation(null)
            .caseAccessCategory(null)
            .defendant2PartyName(EMPTY)
            .generalAppSuperClaimType(EMPTY)
            .caseManagementCategory(GACaseManagementCategory.builder().build())
            .locationName(EMPTY)
            .generalAppHearingDate(GAHearingDateGAspec.builder().build())
            .applicantPartyName(EMPTY).build();

        return applicationBuilder.build();
    }

    private List<Element<GeneralApplication>> addApplication(GeneralApplication application,
                                                             List<Element<GeneralApplication>>
                                                                 generalApplicationsList) {
        List<Element<GeneralApplication>> newApplication = newArrayList();
        newApplication.addAll(generalApplicationsList);
        newApplication.add(element(application));

        return newApplication;
    }

    private List<Element<GADetailsRespondentSol>> updateGaDetailsRespondentOne(CaseData caseData, String newState,
                                                                               String applicationId) {
        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol = ofNullable(
            caseData.getRespondentSolGaAppDetails()).orElse(newArrayList());
        if (!isEmpty(gaDetailsRespondentSol)
            && gaDetailsRespondentSol.stream()
                .anyMatch(respondentOne -> gaRespSolAppFilterCriteria(respondentOne, applicationId))) {

            gaDetailsRespondentSol.stream()
                    .filter(respondentOne -> gaRespSolAppFilterCriteria(respondentOne, applicationId))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new)
                    .getValue().setCaseState(newState);
        }
        return gaDetailsRespondentSol;
    }

    private List<Element<GADetailsRespondentSol>> updateGaDetailsRespondentTwo(CaseData caseData, String newState,
                                                                               String applicationId) {
        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolTwo = ofNullable(
            caseData.getRespondentSolTwoGaAppDetails()).orElse(newArrayList());

        if (!isEmpty(gaDetailsRespondentSolTwo)
            && gaDetailsRespondentSolTwo.stream()
                .anyMatch(respondentTwo -> gaRespSolAppFilterCriteria(respondentTwo, applicationId))) {

            gaDetailsRespondentSolTwo.stream()
                    .filter(respondentTwo -> gaRespSolAppFilterCriteria(respondentTwo, applicationId))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new)
                    .getValue().setCaseState(newState);
        }
        return gaDetailsRespondentSolTwo;
    }

    private boolean applicationFilterCriteria(Element<GeneralApplicationsDetails> gaDetails, String applicationId) {
        return gaDetails.getValue() != null
            && gaDetails.getValue().getCaseLink() != null
            && applicationId.equals(gaDetails.getValue().getCaseLink().getCaseReference());
    }

    private boolean applicationRespFilterCriteria(Element<GADetailsRespondentSol> gaDetails, String applicationId) {
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
                                                   List<Element<GeneralApplication>> civilGeneralApplications,
                                                   List<Element<GeneralApplicationsDetails>> claimantGaAppDetails,
                                                   List<Element<GADetailsRespondentSol>> respondentSolGaAppDetails,
                                                   List<Element<GADetailsRespondentSol>>
                                                       respondentSolTwoGaAppDetails,
                                                   List<Element<GeneralApplicationsDetails>>
                                                       gaDetailsMasterCollection) {
        Map<String, Object> output = caseData.toMap(mapper);
        output.put("generalApplications", civilGeneralApplications);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_CLAIMANT, claimantGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL, respondentSolGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL_TWO, respondentSolTwoGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_JUDGE, gaDetailsMasterCollection);
        return output;
    }

}
