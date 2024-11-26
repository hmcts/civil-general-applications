package uk.gov.hmcts.reform.civil.handler.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.utils.CaseDataContentConverter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Long.parseLong;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateFromGACaseEventHandler {

    private static final String GA_DOC_SUFFIX = "Document";
    private static final String GA_ADDL_DOC_SUFFIX = "Doc";
    private static final String CIVIL_DOC_STAFF_SUFFIX = "DocStaff";
    private static final String CIVIL_DOC_CLAIMANT_SUFFIX = "DocClaimant";
    private static final String CIVIL_DOC_RESPONDENT_SOL_SUFFIX = "DocRespondentSol";
    private static final String CIVIL_DOC_RESPONDENT_SOL_TWO_SUFFIX = "DocRespondentSolTwo";
    private static final String GA_DRAFT = "gaDraft";

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper mapper;
    private final FeatureToggleService featureToggleService;

    public void handleEventUpdate(CallbackParams callbackParams, CaseEvent caseEvent) {

        CaseData generalAppCaseData = callbackParams.getCaseData();
        String parentCaseId = generalAppCaseData.getGeneralAppParentCaseLink().getCaseReference();
        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(
                parentCaseId,
                caseEvent
        );

        var caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        coreCaseDataService.submitUpdate(
                parentCaseId,
                CaseDataContentConverter.caseDataContentFromStartEventResponse(
                        startEventResponse,
                        getUpdatedCaseData(caseData, generalAppCaseData)
                )
        );
    }

    private Map<String, Object> getUpdatedCaseData(CaseData civilCaseData, CaseData generalAppCaseData) {
        Map<String, Object> output = civilCaseData.toMap(mapper);
        try {
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "generalOrder");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "dismissalOrder");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "directionOrder");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "hearingNotice");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "generalAppEvidence");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "hearingOrder");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "requestForInformation");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "writtenRepSequential");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "writtenRepConcurrent");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "consentOrder");
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, GA_DRAFT);
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "gaResp");
            updateDocCollection(output, generalAppCaseData, "gaRespondDoc", civilCaseData, "gaRespondDoc");
            generalAppCaseData = mergeBundle(generalAppCaseData);
            updateDocCollectionField(output, civilCaseData, generalAppCaseData, "gaAddl");

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return output;
    }

    protected CaseData mergeBundle(CaseData generalAppCaseData) {
        if (Objects.nonNull(generalAppCaseData.getGaAddlDocBundle())) {
            List<Element<CaseDocument>> newGaAddlDoc = generalAppCaseData.getGaAddlDoc();
            if (Objects.isNull(newGaAddlDoc)) {
                newGaAddlDoc = new ArrayList<>();
            }
            newGaAddlDoc.addAll(generalAppCaseData.getGaAddlDocBundle());
            return generalAppCaseData.toBuilder().gaAddlDoc(newGaAddlDoc).build();
        }
        return generalAppCaseData;
    }

    protected void updateDocCollectionField(Map<String, Object> output, CaseData civilCaseData, CaseData generalAppCaseData, String docFieldName)
            throws Exception {
        String civilDocPrefix = docFieldName;
        if (civilDocPrefix.equals("generalAppEvidence")) {
            civilDocPrefix = "gaEvidence";
        }

        if (civilDocPrefix.equals("requestForInformation")) {
            civilDocPrefix = "requestForInfo";
        }

        if (civilDocPrefix.equals("writtenRepSequential")) {
            civilDocPrefix = "writtenRepSeq";
        }

        if (civilDocPrefix.equals("writtenRepConcurrent")) {
            civilDocPrefix = "writtenRepCon";
        }

        //staff collection will hold ga doc accessible for judge and staff
        String fromGaList = docFieldName + GA_DOC_SUFFIX;
        if (civilDocPrefix.equals("gaAddl")) {
            fromGaList = docFieldName + GA_ADDL_DOC_SUFFIX;
        }

        String toCivilStaffList = civilDocPrefix + CIVIL_DOC_STAFF_SUFFIX;
        updateDocCollection(output, generalAppCaseData, fromGaList,
                civilCaseData, toCivilStaffList);
        //Claimant collection will hold ga doc accessible for Claimant
        String toCivilClaimantList = civilDocPrefix + CIVIL_DOC_CLAIMANT_SUFFIX;
        if (canViewClaimant(civilCaseData, generalAppCaseData)) {
            updateDocCollection(output, generalAppCaseData, fromGaList,
                    civilCaseData, toCivilClaimantList);
        }
        //RespondentSol collection will hold ga doc accessible for RespondentSol1
        String toCivilRespondentSol1List = civilDocPrefix + CIVIL_DOC_RESPONDENT_SOL_SUFFIX;
        if (canViewResp(civilCaseData, generalAppCaseData, "1")) {
            updateDocCollection(output, generalAppCaseData, fromGaList,
                    civilCaseData, toCivilRespondentSol1List);
        }
        //Respondent2Sol collection will hold ga doc accessible for RespondentSol2
        String toCivilRespondentSol2List = civilDocPrefix + CIVIL_DOC_RESPONDENT_SOL_TWO_SUFFIX;
        if (canViewResp(civilCaseData, generalAppCaseData, "2")) {
            updateDocCollection(output, generalAppCaseData, fromGaList,
                    civilCaseData, toCivilRespondentSol2List);
        }
    }

    /**
     * Update GA document collection at civil case.
     *
     * @param output             output map for update civil case.
     * @param civilCaseData      civil case data.
     * @param generalAppCaseData GA case data.
     * @param fromGaList         base ga field name.
     *                           when get from GA data,
     *                           add 'get' to the name then call getter to access related GA document field.
     * @param toCivilList        base civil field name.
     *                           when get from Civil data,
     *                           add 'get' to the name then call getter to access related Civil document field.
     *                           when update output, use name as key to hold to-be-update collection
     */
    @SuppressWarnings({"unchecked", "java:S3776"})
    protected void updateDocCollection(Map<String, Object> output, CaseData generalAppCaseData, String fromGaList,
                                       CaseData civilCaseData, String toCivilList) throws Exception {
        Method gaGetter = ReflectionUtils.findMethod(CaseData.class,
                "get" + StringUtils.capitalize(fromGaList));
        List<Element<?>> gaDocs =
                (List<Element<?>>) (gaGetter != null ? gaGetter.invoke(generalAppCaseData) : null);
        Method civilGetter = ReflectionUtils.findMethod(CaseData.class,
                "get" + StringUtils.capitalize(toCivilList));
        List<Element<?>> civilDocs =
                (List<Element<?>>) ofNullable(civilGetter != null ? civilGetter.invoke(civilCaseData) : null)
                        .orElse(newArrayList());
        if (gaDocs != null && !(fromGaList.equals("gaDraftDocument"))) {
            List<UUID> ids = civilDocs.stream().map(Element::getId).toList();
            for (Element<?> gaDoc : gaDocs) {
                if (!ids.contains(gaDoc.getId())) {
                    civilDocs.add(gaDoc);
                }
            }
        } else if (featureToggleService.isGaForLipsEnabled() && (civilCaseData.isRespondent1LiP() || civilCaseData.isRespondent2LiP()
                || civilCaseData.isApplicantNotRepresented()) && (gaDocs != null && (fromGaList.equals("gaDraftDocument")))) {

            checkDraftDocumentsInMainCase(civilDocs, gaDocs);
        } else {
            if (gaDocs != null && gaDocs.size() == 1 && checkIfDocumentExists(civilDocs, gaDocs) < 1) {
                civilDocs.addAll(gaDocs);
            }
        }
        output.put(toCivilList, civilDocs.isEmpty() ? null : civilDocs);
    }

    protected List<Element<?>> checkDraftDocumentsInMainCase(List<Element<?>> civilDocs, List<Element<?>> gaDocs) {
        List<UUID> ids = gaDocs.stream().map(Element::getId).toList();
        List<Element<?>> civilDocsCopy = newArrayList();

        for (Element<?> civilDoc : civilDocs) {
            if (!ids.contains(civilDoc.getId())) {
                civilDocsCopy.add(civilDoc);
            }
        }

        List<UUID> civilIds = civilDocs.stream().map(Element::getId).toList();
        for (Element<?> gaDoc : gaDocs) {
            if (!civilIds.contains(gaDoc.getId())) {
                civilDocsCopy.add(gaDoc);
            }
        }

        civilDocs.clear();
        civilDocs.addAll(civilDocsCopy);
        civilDocsCopy.clear();

        return civilDocs;
    }

    protected boolean canViewClaimant(CaseData civilCaseData, CaseData generalAppCaseData) {
        List<Element<GeneralApplicationsDetails>> gaAppDetails = civilCaseData.getClaimantGaAppDetails();
        if (isNull(gaAppDetails)) {
            return false;
        }

        return gaAppDetails.stream()
                .anyMatch(civilGaData -> generalAppCaseData.getCcdCaseReference()
                        .equals(parseLong(civilGaData.getValue().getCaseLink().getCaseReference())));
    }

    protected boolean canViewResp(CaseData civilCaseData, CaseData generalAppCaseData, String respondent) {
        List<Element<GADetailsRespondentSol>> gaAppDetails;
        if (respondent.equals("2")) {
            gaAppDetails = civilCaseData.getRespondentSolTwoGaAppDetails();
        } else {
            gaAppDetails = civilCaseData.getRespondentSolGaAppDetails();
        }
        if (isNull(gaAppDetails)) {
            return false;
        }

        return gaAppDetails.stream()
                .anyMatch(civilGaData -> generalAppCaseData.getCcdCaseReference()
                        .equals(parseLong(civilGaData.getValue().getCaseLink().getCaseReference())));
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
                    .parallelStream().anyMatch(gaDocument -> gaDocument.getValue().getDocumentLink().getDocumentUrl()
                            .equals(civilDocument.getValue().getDocumentLink().getDocumentUrl()))).toList().size();
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
}
