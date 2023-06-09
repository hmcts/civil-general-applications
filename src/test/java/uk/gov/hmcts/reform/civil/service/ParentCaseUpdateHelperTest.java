package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;

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
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
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
    void updateParentWithGAState_Respond_Doc() {
        assertThat(DOCUMENT_STATES.size()).isEqualTo(3);
        assertThat(DOCUMENT_STATES.contains(AWAITING_ADDITIONAL_INFORMATION)).isTrue();
        assertThat(DOCUMENT_STATES.contains(AWAITING_WRITTEN_REPRESENTATIONS)).isTrue();
        assertThat(DOCUMENT_STATES.contains(AWAITING_DIRECTIONS_ORDER_DOCS)).isTrue();
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
}
