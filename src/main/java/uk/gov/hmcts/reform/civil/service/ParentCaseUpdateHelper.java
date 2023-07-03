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
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
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

    protected static List<CaseState> DOCUMENT_STATES = Arrays.asList(
            AWAITING_ADDITIONAL_INFORMATION,
            AWAITING_WRITTEN_REPRESENTATIONS,
            AWAITING_DIRECTIONS_ORDER_DOCS
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
        Map<String, Object> updateMap = getUpdatedCaseData(caseData, generalApplications,
                respondentSpecficGADetails,
                respondentSpecficGADetailsTwo,
                gaDetailsMasterCollection);
        if (DOCUMENT_STATES.contains(generalAppCaseData.getCcdState())) {
            updateCaseDocument(updateMap, caseData, generalAppCaseData, docVisibilityRoles);
        }
        if (Objects.nonNull(generalAppCaseData.getGeneralAppEvidenceDocument())
            && !generalAppCaseData.getGeneralAppEvidenceDocument().isEmpty()) {
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
            Map<String, Object> updateMap = getUpdatedCaseData(caseData, gaDetailsClaimant,
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

    private boolean gaRespSolAppFilterCriteria(Element<GADetailsRespondentSol> gaDetails, String applicationId) {
        return gaDetails.getValue() != null
            && gaDetails.getValue().getCaseLink() != null
            && applicationId.equals(gaDetails.getValue().getCaseLink().getCaseReference());
    }

    private Map<String, Object> getUpdatedCaseData(CaseData caseData,
                                                   List<Element<GeneralApplicationsDetails>> claimantGaAppDetails,
                                                   List<Element<GADetailsRespondentSol>> respondentSolGaAppDetails,
                                                   List<Element<GADetailsRespondentSol>>
                                                       respondentSolTwoGaAppDetails,
                                                   List<Element<GeneralApplicationsDetails>>
                                                       gaDetailsMasterCollection) {
        Map<String, Object> output = caseData.toMap(mapper);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_CLAIMANT, claimantGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL, respondentSolGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL_TWO, respondentSolTwoGaAppDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_JUDGE, gaDetailsMasterCollection);
        return output;
    }
}
