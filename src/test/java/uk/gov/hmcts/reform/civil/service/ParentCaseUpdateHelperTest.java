package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_APPLICATION_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_APPLICATION_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper.DOCUMENT_STATES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SpringBootTest(classes = {
    ParentCaseUpdateHelper.class,
    ObjectMapper.class,
})
class ParentCaseUpdateHelperTest {

    @Autowired
    ParentCaseUpdateHelper parentCaseUpdateHelper;
    @MockBean
    CoreCaseDataService coreCaseDataService;
    @MockBean
    CaseDetailsConverter caseDetailsConverter;
    @Autowired
    ObjectMapper objectMapper;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Test
    void updateParentApplicationVisibilityWithNewState() {
        CaseData caseData = CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication(NO)
            .submittedOn(null).build();

        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);

        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(caseData, ORDER_MADE.toString());
        verify(coreCaseDataService, times(1)).submitUpdate(any(), any());
    }

    @Test
    void updateParentApplicationVisibilityWithNewStateWithoutRespondentSol() {
        CaseData caseData = getCaseWithApplicationData(false);
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);

        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(caseData, ORDER_MADE.toString());
        verify(coreCaseDataService, times(1)).submitUpdate(any(), any());

    }

    @Test
    void updateParentApplicationVisibilityWithNewStateWithRespondentSol() {
        CaseData caseData = getCaseWithApplicationData(true);
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);

        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(caseData, ORDER_MADE.toString());
        verify(coreCaseDataService, times(1)).submitUpdate(any(), any());

    }

    @Test
    void updateCaseDocumentByType_null() {
        CaseData gaCase = getCaseWithApplicationDataAndGeneralOrder();
        CaseData civilCase = getCaseWithApplicationData(false);
        Map<String, Object> updateMap = new HashMap<>();
        try {
            parentCaseUpdateHelper.updateCaseDocumentByType(updateMap, "directionOrder", null,
                    civilCase, gaCase);
            assertThat(updateMap.size()).isZero();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void updateCaseDocumentByType() {
        CaseData gaCase = getCaseWithApplicationDataAndGeneralOrder();
        CaseData civilCase = getCaseWithApplicationData(false);
        Map<String, Object> updateMap = new HashMap<>();
        try {
            parentCaseUpdateHelper.updateCaseDocumentByType(updateMap, "directionOrder", "RespondentSol",
                    civilCase, gaCase);
            assertThat(updateMap).isNotNull();
            assertThat(updateMap.get("directionOrderDocRespondentSol")).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void updateCaseDocumentByRole() {
        CaseData gaCase = getCaseWithApplicationDataAndGeneralOrder();
        CaseData civilCase = getCaseWithApplicationData(false);
        Map<String, Object> updateMap = new HashMap<>();
        parentCaseUpdateHelper.updateCaseDocumentByRole(updateMap, "RespondentSol",
                civilCase, gaCase);
        assertThat(updateMap).isNotNull();
        assertThat(updateMap.get("directionOrderDocRespondentSol")).isNotNull();
    }

    @Test
    void updateCaseDocument() {
        CaseData gaCase = getCaseWithApplicationDataAndGeneralOrder();
        CaseData civilCase = getCaseWithApplicationData(false);
        Map<String, Object> updateMap = new HashMap<>();
        String[] roles = {"Claimant", "RespondentSol", null};
        parentCaseUpdateHelper.updateCaseDocument(updateMap,
                civilCase, gaCase, roles);
        assertThat(updateMap).isNotNull();
        assertThat(updateMap.get("directionOrderDocRespondentSol")).isNotNull();
        assertThat(updateMap.get("directionOrderDocClaimant")).isNotNull();
    }

    @Test
    void checkIfDocumentExists() {
        Element<?> same = Element.<CaseDocument>builder()
            .id(UUID.randomUUID())
            .value(CaseDocument.builder().documentLink(Document.builder().documentUrl("string").build())
                       .build()).build();
        List<Element<?>> gaDocumentList = new ArrayList<>();
        List<Element<?>> civilCaseDocumentList = new ArrayList<>();
        gaDocumentList.add(same);
        assertThat(parentCaseUpdateHelper.checkIfDocumentExists(civilCaseDocumentList, gaDocumentList)).isEqualTo(0);
        civilCaseDocumentList.add(same);
        assertThat(parentCaseUpdateHelper.checkIfDocumentExists(civilCaseDocumentList, gaDocumentList)).isEqualTo(1);
    }

    @Test
    void checkIfDocumentExists_whenDocumentTypeIsDocumentClass() {
        Element<Document> documentElement = Element.<Document>builder()
            .id(UUID.randomUUID())
            .value(Document.builder().documentUrl("string").build()).build();
        List<Element<?>> gaDocumentList = new ArrayList<>();
        List<Element<?>> civilCaseDocumentList = new ArrayList<>();
        gaDocumentList.add(documentElement);
        assertThat(parentCaseUpdateHelper.checkIfDocumentExists(civilCaseDocumentList, gaDocumentList)).isEqualTo(0);
        civilCaseDocumentList.add(documentElement);
        assertThat(parentCaseUpdateHelper.checkIfDocumentExists(civilCaseDocumentList, gaDocumentList)).isEqualTo(1);
    }

    @Test
    void updateParentWithGAState_Respond_Doc() {
        assertThat(DOCUMENT_STATES.size()).isEqualTo(4);
        assertThat(DOCUMENT_STATES.contains(AWAITING_ADDITIONAL_INFORMATION)).isTrue();
        assertThat(DOCUMENT_STATES.contains(AWAITING_WRITTEN_REPRESENTATIONS)).isTrue();
        assertThat(DOCUMENT_STATES.contains(AWAITING_DIRECTIONS_ORDER_DOCS)).isTrue();
        assertThat(DOCUMENT_STATES.contains(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)).isTrue();
    }

    @Test
    void findGaCreator() {
        String role = "Claimant";
        assertThat(parentCaseUpdateHelper
                .findGaCreator(getVaryMainCaseData(role),
                         getGaVaryCaseData(role, PENDING_APPLICATION_ISSUED)))
                .isEqualTo(role);
        role = "RespondentSol";
        assertThat(parentCaseUpdateHelper
                .findGaCreator(getVaryMainCaseData(role),
                        getGaVaryCaseData(role, PENDING_APPLICATION_ISSUED)))
                .isEqualTo(role);
        role = "RespondentSolTwo";
        assertThat(parentCaseUpdateHelper
                .findGaCreator(getVaryMainCaseData(role),
                        getGaVaryCaseData(role, PENDING_APPLICATION_ISSUED)))
                .isEqualTo(role);
        assertThat(parentCaseUpdateHelper
                .findGaCreator(getVaryMainCaseData(role),
                        getGaVaryCaseData(role, PENDING_APPLICATION_ISSUED).toBuilder()
                                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                        .organisationIdentifier("Nothing").build()).build()))
                .isNull();
    }

    @Test
    void updateSingleTypeByRoles() {
        CaseData gaCase = getCaseWithApplicationDataAndGeneralOrder();
        CaseData civilCase = getCaseWithApplicationData(false);
        Map<String, Object> updateMap = new HashMap<>();
        String[] roles = {"RespondentSol", "Staff"};
        try {
            parentCaseUpdateHelper.updateSingleTypeByRoles(updateMap, "directionOrder", roles,
                    civilCase, gaCase);
            assertThat(updateMap).isNotNull();
            assertThat(updateMap.get("directionOrderDocRespondentSol")).isNotNull();
            assertThat(updateMap.get("directionOrderDocStaff")).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void updateN245_before() {
        Map<String, Object> updateMap = new HashMap<>();
        String role = "RespondentSolTwo";
        String[] docVisibilityRoles = {"Claimant", "RespondentSol", "RespondentSolTwo", "Staff"};
        parentCaseUpdateHelper.updateEvidence(updateMap, getVaryMainCaseData(role),
                getGaVaryCaseData(role, PENDING_APPLICATION_ISSUED), docVisibilityRoles);
        assertThat(updateMap).isNotNull();
        assertThat(updateMap.get("gaEvidenceDocRespondentSolTwo")).isNotNull();
        assertThat(updateMap.get("gaEvidenceDocStaff")).isNull();
        assertThat(updateMap.get("gaEvidenceDocRespondentSol")).isNull();
        assertThat(updateMap.get("gaEvidenceDocClaimant")).isNull();
    }

    @Test
    void updateN245_after() {
        Map<String, Object> updateMap = new HashMap<>();
        String role = "RespondentSolTwo";
        String[] docVisibilityRoles = {"Claimant", "RespondentSol", "RespondentSolTwo", "Staff"};
        parentCaseUpdateHelper.updateEvidence(updateMap, getVaryMainCaseData(role),
                getGaVaryCaseData(role, AWAITING_APPLICATION_PAYMENT), docVisibilityRoles);
        assertThat(updateMap).isNotNull();
        assertThat(updateMap.get("gaEvidenceDocRespondentSolTwo")).isNotNull();
        assertThat(updateMap.get("gaEvidenceDocStaff")).isNotNull();
        assertThat(updateMap.get("gaEvidenceDocRespondentSol")).isNotNull();
        assertThat(updateMap.get("gaEvidenceDocClaimant")).isNotNull();
    }

    @Test
    void updateParentWithGAState_with_n245_after_payment() {
        CaseData gaCase = getGaVaryCaseData("RespondentSolTwo", AWAITING_APPLICATION_PAYMENT);
        CaseData civilCase = getVaryMainCaseData("RespondentSolTwo");
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCase);
        parentCaseUpdateHelper.updateParentWithGAState(gaCase, AWAITING_RESPONDENT_RESPONSE.toString());
        verify(coreCaseDataService, times(1))
                .caseDataContentFromStartEventResponse(any(), mapCaptor.capture());
        assertThat(mapCaptor.getValue().get("gaEvidenceDocRespondentSolTwo")).isNotNull();
        assertThat(mapCaptor.getValue().get("gaEvidenceDocStaff")).isNotNull();
        assertThat(mapCaptor.getValue().get("gaEvidenceDocRespondentSol")).isNotNull();
        assertThat(mapCaptor.getValue().get("gaEvidenceDocClaimant")).isNotNull();
    }

    @Test
    void updateParentWithGAState_with_n245_before_payment() {
        CaseData gaCase = getGaVaryCaseData("RespondentSolTwo", PENDING_APPLICATION_ISSUED);
        CaseData civilCase = getVaryMainCaseData("RespondentSolTwo");
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCase);
        parentCaseUpdateHelper.updateParentWithGAState(gaCase, AWAITING_APPLICATION_PAYMENT.toString());
        verify(coreCaseDataService, times(1))
                .caseDataContentFromStartEventResponse(any(), mapCaptor.capture());
        assertThat(mapCaptor.getValue().get("gaEvidenceDocRespondentSolTwo")).isNotNull();
        assertThat(mapCaptor.getValue().get("gaEvidenceDocStaff")).isNull();
        assertThat(mapCaptor.getValue().get("gaEvidenceDocRespondentSol")).isNull();
        assertThat(mapCaptor.getValue().get("gaEvidenceDocClaimant")).isNull();
    }

    @Test
    void updateParentWithGAState_without_n245() {
        CaseData gaCase = getCaseWithApplicationDataAndGeneralOrder();
        CaseData civilCase = getCaseWithApplicationData(false);
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCase);
        parentCaseUpdateHelper.updateParentWithGAState(gaCase, AWAITING_APPLICATION_PAYMENT.toString());
        verify(coreCaseDataService, times(1))
                .caseDataContentFromStartEventResponse(any(), mapCaptor.capture());
        assertThat(mapCaptor.getValue().get("gaEvidenceDocStaff")).isNull();
    }

    private StartEventResponse getStartEventResponse(YesOrNo isConsented, YesOrNo isTobeNotified) {
        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(
            CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication(NO).build())
            .id(1645779506193000L)
            .state(APPLICATION_ADD_PAYMENT)
            .build();
        StartEventResponse.StartEventResponseBuilder startEventResponseBuilder = StartEventResponse.builder();
        startEventResponseBuilder.eventId(UPDATE_CASE_WITH_GA_STATE.toString())
            .token("BEARER_TOKEN")
            .caseDetails(caseDetails);

        return startEventResponseBuilder.build();
    }

    private CaseData getCaseWithApplicationData(Boolean withRespondentSol) {
        GeneralApplication generalApplication = GeneralApplication
                .builder()
                .caseLink(CaseLink.builder().caseReference(CaseDataBuilder.CASE_ID.toString()).build())
                .build();

        List<Element<GeneralApplicationsDetails>> generalApplicationsDetailsList = Lists.newArrayList();

        GeneralApplicationsDetails generalApplicationsDetails = GeneralApplicationsDetails.builder()
                .generalApplicationType("Summary judgment")
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState("pending").build();
        generalApplicationsDetailsList.add(element(generalApplicationsDetails));

        List<Element<GeneralApplicationsDetails>> gaDetailsMasterCollection = Lists.newArrayList();
        GeneralApplicationsDetails gaDetailsMasterColl = GeneralApplicationsDetails.builder()
            .generalApplicationType("Summary judgment")
            .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
            .caseLink(generalApplication.getCaseLink())
            .caseState("pending").build();
        gaDetailsMasterCollection.add(element(gaDetailsMasterColl));

        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolList = Lists.newArrayList();
        GADetailsRespondentSol gaDetailsRespondentSol = GADetailsRespondentSol.builder()
                .generalApplicationType("Summary judgment")
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState("pending").build();
        gaDetailsRespondentSolList.add(element(gaDetailsRespondentSol));

        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolListTwo = Lists.newArrayList();
        GADetailsRespondentSol gaDetailsRespondentSolTwo = GADetailsRespondentSol.builder()
            .generalApplicationType("Summary judgment")
            .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
            .caseLink(generalApplication.getCaseLink())
            .caseState("pending").build();
        gaDetailsRespondentSolListTwo.add(element(gaDetailsRespondentSolTwo));

        List<Element<GeneralApplication>> generalApplications = wrapElements(generalApplication);

        return CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication(NO)
                .generalApplications(generalApplications)
                .claimantGaAppDetails(generalApplicationsDetailsList)
                .gaDetailsMasterCollection(gaDetailsMasterCollection)
                .respondentSolGaAppDetails(withRespondentSol ? gaDetailsRespondentSolList : null)
                .respondentSolTwoGaAppDetails(withRespondentSol ? gaDetailsRespondentSolListTwo : null)
                .submittedOn(null).build();
    }

    private CaseData getCaseWithApplicationDataAndGeneralOrder() {
        String uid = "f000aa01-0451-4000-b000-000000000000";
        CaseDocument pdfDocument = CaseDocument.builder()
                .createdBy("John")
                .documentName("documentName")
                .documentSize(0L)
                .documentType(GENERAL_ORDER)
                .createdDatetime(now())
                .documentLink(Document.builder()
                        .documentUrl("fake-url")
                        .documentFileName("file-name")
                        .documentBinaryUrl("binary-url")
                        .build())
                .build();
        return getCaseWithApplicationData(false)
                .toBuilder().directionOrderDocument(singletonList(Element.<CaseDocument>builder()
                        .id(UUID.fromString(uid))
                        .value(pdfDocument).build())).build();
    }

    private CaseData getGaVaryCaseData(String role, CaseState state) {
        CaseData.CaseDataBuilder builder = CaseData.builder();
        builder.generalAppType(GAApplicationType.builder()
                .types(List.of(GeneralApplicationTypes.VARY_JUDGEMENT)).build())
                .ccdCaseReference(CaseDataBuilder.CASE_ID)
                .generalAppParentCaseLink(GeneralAppParentCaseLink
                        .builder().caseReference(CaseDataBuilder.CASE_ID.toString()).build())
                .ccdState(state);
        Document pdfDocument = Document.builder()
                .documentUrl("fake-url")
                .documentFileName("file-name")
                .documentBinaryUrl("binary-url")
                .build();
        String uid = "f000aa01-0451-4000-b000-000000000000";
        builder.generalAppEvidenceDocument(singletonList(Element.<Document>builder()
                .id(UUID.fromString(uid))
                .value(pdfDocument).build()));
        switch (role) {
            case "Claimant":
                builder.parentClaimantIsApplicant(YES);
                break;
            case "RespondentSol":
                builder.parentClaimantIsApplicant(NO).generalAppApplnSolicitor(
                        GASolicitorDetailsGAspec.builder()
                                .organisationIdentifier("RespondentSol").build());
                break;
            case "RespondentSolTwo":
                builder.parentClaimantIsApplicant(NO).isMultiParty(YES).generalAppApplnSolicitor(
                        GASolicitorDetailsGAspec.builder()
                                .organisationIdentifier("RespondentSolTwo").build());
                break;
            default:
                break;
        }
        return builder.build();
    }

    private CaseData getVaryMainCaseData(String role) {
        CaseData.CaseDataBuilder builder = CaseData.builder();

        GeneralApplication generalApplication = GeneralApplication
                .builder()
                .caseLink(CaseLink.builder().caseReference(CaseDataBuilder.CASE_ID.toString()).build())
                .build();

        List<Element<GeneralApplicationsDetails>> generalApplicationsDetailsList = Lists.newArrayList();

        GeneralApplicationsDetails generalApplicationsDetails = GeneralApplicationsDetails.builder()
                .generalApplicationType(GeneralApplicationTypes.VARY_JUDGEMENT.toString())
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState(PENDING_APPLICATION_ISSUED.toString()).build();
        generalApplicationsDetailsList.add(element(generalApplicationsDetails));

        List<Element<GeneralApplicationsDetails>> gaDetailsMasterCollection = Lists.newArrayList();
        GeneralApplicationsDetails gaDetailsMasterColl = GeneralApplicationsDetails.builder()
                .generalApplicationType(GeneralApplicationTypes.VARY_JUDGEMENT.toString())
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState(PENDING_APPLICATION_ISSUED.toString()).build();
        gaDetailsMasterCollection.add(element(gaDetailsMasterColl));

        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolList = Lists.newArrayList();
        GADetailsRespondentSol gaDetailsRespondentSol = GADetailsRespondentSol.builder()
                .generalApplicationType(GeneralApplicationTypes.VARY_JUDGEMENT.toString())
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState(PENDING_APPLICATION_ISSUED.toString()).build();
        gaDetailsRespondentSolList.add(element(gaDetailsRespondentSol));

        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolListTwo = Lists.newArrayList();
        GADetailsRespondentSol gaDetailsRespondentSolTwo = GADetailsRespondentSol.builder()
                .generalApplicationType(GeneralApplicationTypes.VARY_JUDGEMENT.toString())
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState(PENDING_APPLICATION_ISSUED.toString()).build();
        gaDetailsRespondentSolListTwo.add(element(gaDetailsRespondentSolTwo));

        List<Element<GeneralApplication>> generalApplications = wrapElements(generalApplication);
        builder.generalApplications(generalApplications);
        builder.gaDetailsMasterCollection(gaDetailsMasterCollection);
        switch (role) {
            case "Claimant":
                builder.claimantGaAppDetails(generalApplicationsDetailsList);
                break;
            case "RespondentSol":
                builder.respondent1OrganisationPolicy(OrganisationPolicy.builder().build())
                        .respondent1OrganisationIDCopy("RespondentSol");
                builder.claimantGaAppDetails(generalApplicationsDetailsList);
                builder.respondentSolGaAppDetails(gaDetailsRespondentSolList);
                break;
            case "RespondentSolTwo":
                builder.respondent1OrganisationPolicy(OrganisationPolicy.builder().build());
                builder.respondent2OrganisationPolicy(OrganisationPolicy.builder().build())
                        .respondent2OrganisationIDCopy("RespondentSolTwo")
                        .respondent2SameLegalRepresentative(NO)
                        .addApplicant2(NO);
                builder.claimantGaAppDetails(generalApplicationsDetailsList);
                builder.respondentSolGaAppDetails(gaDetailsRespondentSolList);
                builder.respondentSolTwoGaAppDetails(gaDetailsRespondentSolListTwo);
                break;
            default:
                break;
        }
        return builder.build();
    }
}
