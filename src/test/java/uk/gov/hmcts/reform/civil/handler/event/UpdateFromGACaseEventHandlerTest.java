package uk.gov.hmcts.reform.civil.handler.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAStatementOfTruth;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.PDFBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.ADD_PDF_TO_MAIN_CASE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.RELIEF_FROM_SANCTIONS;
import static uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder.CUSTOMER_REFERENCE;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@ExtendWith(MockitoExtension.class)
class UpdateFromGACaseEventHandlerTest extends BaseCallbackHandlerTest {

    private static final Long CHILD_CCD_REF = 1646003133062762L;
    private static final Long PARENT_CCD_REF = 1645779506193000L;
    private static final String STRING_CONSTANT = "STRING_CONSTANT";

    UpdateFromGACaseEventHandler handler;

    @Mock
    CaseDetailsConverter caseDetailsConverter;

    @Mock
    CoreCaseDataService coreCaseDataService;

    ObjectMapper mapper;

    @Mock
    FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        handler = new UpdateFromGACaseEventHandler(caseDetailsConverter, coreCaseDataService, mapper, featureToggleService);
    }

    @Test
    void testShouldAddDocument() {

        CaseData caseData = getSampleGeneralApplicationCaseData(NO, YES);

        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
        StartEventResponse startEventResponse = startEventResponse(caseDetails);

        when(coreCaseDataService.startUpdate(String.valueOf(PARENT_CCD_REF), ADD_PDF_TO_MAIN_CASE)).thenReturn(startEventResponse);
        when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails())).thenReturn(caseData);
        when(coreCaseDataService.submitUpdate(anyString(), any(CaseDataContent.class))).thenReturn(caseData);

        CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
        handler.handleEventUpdate(params, ADD_PDF_TO_MAIN_CASE);

        verify(coreCaseDataService).startUpdate(String.valueOf(PARENT_CCD_REF), ADD_PDF_TO_MAIN_CASE);
        verify(coreCaseDataService).submitUpdate(eq(String.valueOf(PARENT_CCD_REF)), any(CaseDataContent.class));
    }

    @Test
    void checkIfDocumentExists_whenDocumentTypeIsDocumentClass() {
        Element<Document> documentElement = Element.<Document>builder()
                .id(UUID.randomUUID())
                .value(Document.builder().documentUrl("string").build()).build();
        List<Element<?>> gaDocumentList = new ArrayList<>();
        List<Element<?>> civilCaseDocumentList = new ArrayList<>();
        gaDocumentList.add(documentElement);
        assertThat(handler.checkIfDocumentExists(civilCaseDocumentList, gaDocumentList)).isEqualTo(0);
        civilCaseDocumentList.add(documentElement);
        assertThat(handler.checkIfDocumentExists(civilCaseDocumentList, gaDocumentList)).isEqualTo(1);
    }

    @Test
    void shouldMergeBundle() {
        String uid = "f000aa01-0451-4000-b000-000000000000";
        CaseData gaCaseData = new CaseDataBuilder().atStateClaimDraft().build()
                .toBuilder()
                .gaAddlDocBundle(singletonList(Element.<CaseDocument>builder()
                        .id(UUID.fromString(uid))
                        .value(PDFBuilder.GENERAL_ORDER_DOCUMENT).build())).build();
        gaCaseData = handler.mergeBundle(gaCaseData);
        assertThat(gaCaseData.getGaAddlDoc().size()).isEqualTo(1);

        List<Element<CaseDocument>> addlDoc = new ArrayList<Element<CaseDocument>>() {
            {
                add(Element.<CaseDocument>builder()
                    .id(UUID.fromString(uid))
                    .value(PDFBuilder.GENERAL_ORDER_DOCUMENT).build());
            }
        };
        List<Element<CaseDocument>> addlDocBundle = new ArrayList<Element<CaseDocument>>() {
            {
                add(Element.<CaseDocument>builder()
                    .id(UUID.fromString(uid))
                    .value(PDFBuilder.GENERAL_ORDER_DOCUMENT).build());
            }
        };

        gaCaseData = new CaseDataBuilder().atStateClaimDraft().build()
                .toBuilder()
                .gaAddlDoc(addlDoc)
                .gaAddlDocBundle(addlDocBundle).build();
        gaCaseData = handler.mergeBundle(gaCaseData);
        assertThat(gaCaseData.getGaAddlDoc().size()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAddToCivilDocsCopy() {
        CaseData generalCaseData = getSampleGeneralApplicationCaseData(NO, YES);
        generalCaseData = getTestCaseDataWithDraftApplicationPDFDocumentLip(generalCaseData);
        CaseData caseData = CaseDataBuilder.builder().getCivilCaseData();

        Method gaGetter = ReflectionUtils.findMethod(CaseData.class,
                "get" + StringUtils.capitalize("gaDraftDocument"));
        Method civilGetter = ReflectionUtils.findMethod(CaseData.class,
                "get" + StringUtils.capitalize("directionOrderDocStaff"));
        try {
            List<Element<?>> gaDocs =
                    (List<Element<?>>) (gaGetter != null ? gaGetter.invoke(generalCaseData) : null);
            List<Element<?>> civilDocs =
                    (List<Element<?>>) ofNullable(civilGetter != null ? civilGetter.invoke(caseData) : null)
                            .orElse(newArrayList());
            List<Element<?>> civilDocsPre = List.copyOf(civilDocs);
            civilDocs = handler.checkDraftDocumentsInMainCase(civilDocs, gaDocs);
            assertTrue(civilDocs.size() > civilDocsPre.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldUpdateDocCollection() {
        CaseData gaCaseData = new CaseDataBuilder().atStateClaimDraft()
                .build();
        String uid = "f000aa01-0451-4000-b000-000000000000";
        gaCaseData = gaCaseData.toBuilder()
                .directionOrderDocument(singletonList(Element.<CaseDocument>builder()
                        .id(UUID.fromString(uid))
                        .value(PDFBuilder.DIRECTION_ORDER_DOCUMENT).build())).build();
        Map<String, Object> output = new HashMap<>();
        CaseData caseData = new CaseDataBuilder().atStateClaimDraft().build();
        try {
            handler.updateDocCollection(output, gaCaseData, "directionOrderDocument",
                    caseData, "directionOrderDocStaff");
            List<Element<CaseDocument>> toUpdatedDocs =
                    (List<Element<CaseDocument>>) output.get("directionOrderDocStaff");
            assertThat(toUpdatedDocs).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotUpdateNullDocCollection() {
        CaseData gaCaseData = new CaseDataBuilder().atStateClaimDraft()
                .build();
        gaCaseData = gaCaseData.toBuilder().build();
        Map<String, Object> output = new HashMap<>();
        CaseData caseData = new CaseDataBuilder().atStateClaimDraft().build();
        try {
            handler.updateDocCollection(output, gaCaseData, "directionOrderDocument",
                    caseData, "directionOrderDocStaff");
            List<Element<CaseDocument>> toUpdatedDocs =
                    (List<Element<CaseDocument>>) output.get("directionOrderDocStaff");
            assertThat(toUpdatedDocs).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testUpdateDocCollectionWithoutNoticeGaCreatedByResp2() {
        CaseData caseData = getSampleGeneralApplicationCaseData(NO, YES);

        String uid = "f000aa01-0451-4000-b000-000000000000";
        CaseData generalAppCaseData = CaseData.builder()
                .directionOrderDocument(singletonList(Element.<CaseDocument>builder()
                        .id(UUID.fromString(uid))
                        .value(PDFBuilder.DIRECTION_ORDER_DOCUMENT).build())).ccdCaseReference(1234L).build();
        Map<String, Object> output = new HashMap<>();
        try {
            handler.updateDocCollectionField(output, caseData, generalAppCaseData, "directionOrder");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThat(output.get("directionOrderDocStaff")).isNotNull();
        assertThat(output.get("directionOrderDocClaimant")).isNull();
        assertThat(output.get("directionOrderDocRespondentSol")).isNull();
    }

    @Test
    void testCanViewWithoutNoticeGaCreatedByClaimant() {
        CaseData caseData = CaseDataBuilder.builder().getMainCaseDataWithDetails(
                false,
                false,
                false, false).build();

        GeneralApplicationsDetails gaDetails = GeneralApplicationsDetails.builder()
                .caseState(LISTING_FOR_A_HEARING.getDisplayedValue())
                .caseLink(CaseLink.builder()
                        .caseReference(String.valueOf(1234L)).build())
                .build();

        caseData = caseData.toBuilder().claimantGaAppDetails(
                wrapElements(gaDetails
                )).build();

        CaseData generalAppCaseData = CaseData.builder().ccdCaseReference(1234L).build();
        assertThat(handler.canViewResp(caseData, generalAppCaseData, "2")).isFalse();
        assertThat(handler.canViewResp(caseData, generalAppCaseData, "1")).isFalse();
        assertThat(handler.canViewClaimant(caseData, generalAppCaseData)).isTrue();
    }

    private StartEventResponse startEventResponse(CaseDetails caseDetails) {
        return StartEventResponse.builder()
                .token("1594901956117591")
                .eventId(ADD_PDF_TO_MAIN_CASE.name())
                .caseDetails(caseDetails)
                .build();
    }

    private CaseData getSampleGeneralApplicationCaseData(YesOrNo isConsented, YesOrNo isTobeNotified) {
        return CaseDataBuilder.builder().buildCaseDateBaseOnGeneralApplication(
                        getGeneralApplication(isConsented, isTobeNotified))
                .toBuilder().ccdCaseReference(CHILD_CCD_REF).build();
    }

    private GeneralApplication getGeneralApplication(YesOrNo isConsented, YesOrNo isTobeNotified) {
        return GeneralApplication.builder()
                .generalAppType(GAApplicationType.builder().types(List.of(RELIEF_FROM_SANCTIONS)).build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(isConsented).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isTobeNotified).build())
                .generalAppPBADetails(
                        GAPbaDetails.builder()
                                .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .reference("RC-1658-4258-2679-9795")
                                        .customerReference(CUSTOMER_REFERENCE)
                                        .build())
                                .fee(
                                        Fee.builder()
                                                .code("FE203")
                                                .calculatedAmountInPence(BigDecimal.valueOf(27500))
                                                .version("1")
                                                .build())
                                .serviceReqReference(CUSTOMER_REFERENCE).build())
                .generalAppDetailsOfOrder(STRING_CONSTANT)
                .generalAppReasonsOfOrder(STRING_CONSTANT)
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                .generalAppStatementOfTruth(GAStatementOfTruth.builder().build())
                .generalAppHearingDetails(GAHearingDetails.builder().build())
                .generalAppRespondentSolicitors(wrapElements(GASolicitorDetailsGAspec.builder()
                        .email("abc@gmail.com").build()))
                .isMultiParty(NO)
                .isCcmccLocation(YES)
                .caseManagementLocation(GACaseLocation.builder()
                        .baseLocation("687686")
                        .region("4").build())
                .parentClaimantIsApplicant(YES)
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                        .caseReference(PARENT_CCD_REF.toString()).build())
                .build();
    }

    private CaseData getTestCaseDataWithDraftApplicationPDFDocumentLip(CaseData caseData) {
        String uid = "f000aa01-0451-4000-b000-000000000111";
        String uid1 = "f000aa01-0451-4000-b000-000000000000";
        List<Element<CaseDocument>> draftDocs = newArrayList();
        draftDocs.add(Element.<CaseDocument>builder().id(UUID.fromString(uid1))
                .value(PDFBuilder.GENERAL_ORDER_DOCUMENT).build());
        draftDocs.add(Element.<CaseDocument>builder().id(UUID.fromString(uid))
                .value(PDFBuilder.GENERAL_ORDER_DOCUMENT).build());
        return caseData.toBuilder()
                .ccdCaseReference(1234L)
                .generalAppType(GAApplicationType.builder()
                        .types(singletonList(EXTEND_TIME))
                        .build())
                .gaDraftDocument(draftDocs)
                .build();
    }
}