package uk.gov.hmcts.reform.civil.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.FEE_PAYMENT_OUTCOME_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INVALID_HWF_REFERENCE_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MORE_INFORMATION_HWF_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.PARTIAL_REMISSION_HWF_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_HELP_WITH_FEE_NUMBER_GA;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_APPLICATION_PAYMENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.APPLICANT_NAME;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.CASE_REFERENCE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.CASE_TITLE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.CLAIMANT_NAME;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.FEE_AMOUNT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HWF_MORE_INFO_DATE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HWF_MORE_INFO_DOCUMENTS;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HWF_MORE_INFO_DOCUMENTS_WELSH;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HWF_REFERENCE_NUMBER;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.NO_REMISSION_REASONS;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.NO_REMISSION_REASONS_WELSH;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.PART_AMOUNT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.REMAINING_AMOUNT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.TYPE_OF_FEE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.TYPE_OF_FEE_WELSH;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.formatLocalDate;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.enums.HwFMoreInfoRequiredDocuments;
import uk.gov.hmcts.reform.civil.enums.NoRemissionDetailsSummary;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.citizenui.RespondentLiPResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesMoreInformation;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HwfNotificationServiceTest {

    private static final String EMAIL_TEMPLATE_MORE_INFO_HWF = "test-hwf-more-info-id";
    private static final String EMAIL_TEMPLATE_INVALID_HWF_REFERENCE = "test-hwf-invalidrefnumber-id";
    private static final String EMAIL_TEMPLATE_UPDATE_REF_NUMBER = "test-hwf-updaterefnumber-id";
    private static final String EMAIL_TEMPLATE_HWF_PARTIAL_REMISSION = "test-hwf-partialRemission-id";
    private static final String EMAIL_TEMPLATE_NO_REMISSION = "test-hwf-noRemission-id";
    private static final String EMAIL_TEMPLATE_HWF_PAYMENT_OUTCOME = "test-hwf-paymentoutcome-id";
    private static final String EMAIL_TEMPLATE_MORE_INFO_HWF_BILINGUAL = "test-hwf-more-info-bilingual-id";
    private static final String EMAIL_TEMPLATE_INVALID_HWF_REFERENCE_BILINGUAL = "test-hwf-invalidrefnumber-bilingual-id";
    private static final String EMAIL_TEMPLATE_UPDATE_REF_NUMBER_BILINGUAL = "test-hwf-updaterefnumber-bilingual-id";
    private static final String EMAIL_TEMPLATE_HWF_PARTIAL_REMISSION_BILINGUAL = "test-hwf-partialRemission-bilingual-id";
    private static final String EMAIL_TEMPLATE_NO_REMISSION_BILINGUAL = "test-hwf-noremission-bilingual-id";
    private static final String EMAIL_TEMPLATE_HWF_PAYMENT_OUTCOME_WLESH = "test-hwf-paymentoutcome-welsh-id";
    private static final String EMAIL = "test@email.com";
    private static final String APPLICANT = "Mr. John Rambo";
    private static final String CLAIMANT = "Mr. John Rambo";
    private static final String DEFENDANT = "Mr. Joe Doe";
    private static final String GA_REFERENCE = "1111222233334444";
    private static final String HWF_REFERENCE = "000HWF001";
    private static final String REFERENCE_NUMBER = "1";
    private static final String REMISSION_AMOUNT = "100000.00";
    private static final String OUTSTANDING_AMOUNT_IN_POUNDS = "500.00";
    private static final LocalDate NOW = LocalDate.now();

    @Mock
    private NotificationsProperties notificationsProperties;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private CaseDetailsConverter caseDetailsConverter;
    @Mock
    private SolicitorEmailValidation solicitorEmailValidation;
    @InjectMocks
    private HwfNotificationService service;

    private static final CaseData GA_CASE_DATA = CaseDataBuilder.builder()
            .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("1").build())
            .ccdState(AWAITING_APPLICATION_PAYMENT)
            .parentClaimantIsApplicant(YesOrNo.YES)
            .ccdCaseReference(1111222233334444L)
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                    .email(EMAIL)
                    .build()).build()
            .toBuilder()
            .applicantPartyName(APPLICANT)
            .claimant1PartyName(CLAIMANT)
            .defendant1PartyName(DEFENDANT)
            .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber(
                    HWF_REFERENCE).build())
            .generalAppPBADetails(GAPbaDetails.builder()
                    .fee(Fee.builder().calculatedAmountInPence(BigDecimal.valueOf(100000)).build())
                    .build())
            .hwfFeeType(FeeType.APPLICATION)
            .build();

    private static final CaseData ADDITIONAL_CASE_DATA = CaseDataBuilder.builder()
            .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("1").build())
            .ccdState(APPLICATION_ADD_PAYMENT)
            .ccdCaseReference(1111222233334444L)
            .parentClaimantIsApplicant(YesOrNo.YES)
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                    .email(EMAIL)
                    .build()).build()
            .toBuilder()
            .applicantPartyName(APPLICANT)
            .claimant1PartyName(CLAIMANT)
            .defendant1PartyName(DEFENDANT)
            .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber(
                    HWF_REFERENCE).build())
            .generalAppPBADetails(GAPbaDetails.builder()
                    .fee(Fee.builder().calculatedAmountInPence(BigDecimal.valueOf(100000)).build())
                    .build())
            .hwfFeeType(FeeType.ADDITIONAL)
            .build();

    private static final CaseData CIVIL_WELSH_CLA = CaseData.builder().claimantBilingualLanguagePreference("WELSH").build();
    private static final CaseData CIVIL_WELSH_DEF = CaseData.builder().respondent1LiPResponse(
        RespondentLiPResponse.builder().respondent1ResponseLanguage("BOTH").build()
    ).build();

    @BeforeEach
    void setup() {
        when(coreCaseDataService.getCase(any())).thenReturn(CaseDetails.builder().build());
        when(notificationsProperties.getNotifyApplicantForHwFMoreInformationNeeded()).thenReturn(
                EMAIL_TEMPLATE_MORE_INFO_HWF);
        when(notificationsProperties.getNotifyApplicantForHwfUpdateRefNumber()).thenReturn(
                EMAIL_TEMPLATE_UPDATE_REF_NUMBER);
        when(notificationsProperties.getNotifyApplicantForHwfInvalidRefNumber()).thenReturn(
                EMAIL_TEMPLATE_INVALID_HWF_REFERENCE);
        when(notificationsProperties.getNotifyApplicantForHwfPartialRemission()).thenReturn(
                EMAIL_TEMPLATE_HWF_PARTIAL_REMISSION);
        when(notificationsProperties.getNotifyApplicantForNoRemission()).thenReturn(
            EMAIL_TEMPLATE_NO_REMISSION);
        when(notificationsProperties.getNotifyApplicantForHwfPaymentOutcome()).thenReturn(
                EMAIL_TEMPLATE_HWF_PAYMENT_OUTCOME);
        when(notificationsProperties.getLipGeneralAppApplicantEmailTemplateInWelsh()).thenReturn(
            EMAIL_TEMPLATE_HWF_PAYMENT_OUTCOME_WLESH);

	when(notificationsProperties.getNotifyApplicantForHwFMoreInformationNeededWelsh()).thenReturn(
            EMAIL_TEMPLATE_MORE_INFO_HWF_BILINGUAL);
        when(notificationsProperties.getNotifyApplicantForHwfUpdateRefNumberBilingual()).thenReturn(
            EMAIL_TEMPLATE_UPDATE_REF_NUMBER_BILINGUAL);
        when(notificationsProperties.getNotifyApplicantForHwfInvalidRefNumberBilingual()).thenReturn(
            EMAIL_TEMPLATE_INVALID_HWF_REFERENCE_BILINGUAL);
        when(notificationsProperties.getNotifyApplicantForHwfPartialRemissionBilingual()).thenReturn(
            EMAIL_TEMPLATE_HWF_PARTIAL_REMISSION_BILINGUAL);
        when(notificationsProperties.getNotifyApplicantForHwfNoRemissionWelsh()).thenReturn(
            EMAIL_TEMPLATE_NO_REMISSION_BILINGUAL);
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_MoreInformation_Ga() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(MORE_INFORMATION_HWF_GA).build();
        CaseData caseData = GA_CASE_DATA.toBuilder()
                .helpWithFeesMoreInformationGa(HelpWithFeesMoreInformation.builder()
                        .hwFMoreInfoDocumentDate(NOW)
                        .hwFMoreInfoRequiredDocuments(
                                getMoreInformationDocumentList()).build())
                .parentCaseReference(GA_REFERENCE)
                .gaHwfDetails(hwfeeDetails).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-more-info-id",
                getNotificationDataMapMoreInfoGa(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_MoreInformation_Ga_Bilingual() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(MORE_INFORMATION_HWF_GA).build();
        CaseData caseData = GA_CASE_DATA.toBuilder()
            .helpWithFeesMoreInformationGa(HelpWithFeesMoreInformation.builder()
                                               .hwFMoreInfoDocumentDate(NOW)
                                               .hwFMoreInfoRequiredDocuments(
                                                   getMoreInformationDocumentList()).build())
            .gaHwfDetails(hwfeeDetails).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_CLA);
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-more-info-bilingual-id",
            getNotificationDataMapMoreInfoGa(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_MoreInformation_Additional() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(MORE_INFORMATION_HWF_GA).build();
        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder()
                .helpWithFeesMoreInformationAdditional(HelpWithFeesMoreInformation.builder()
                        .hwFMoreInfoDocumentDate(NOW)
                        .hwFMoreInfoRequiredDocuments(
                                getMoreInformationDocumentList()).build())
                .parentCaseReference(GA_REFERENCE)
                .additionalHwfDetails(hwfeeDetails).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-more-info-id",
                getNotificationDataMapMoreInfoAdditional(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_MoreInformation_Additional_Bilingual_Def() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(MORE_INFORMATION_HWF_GA).build();
        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder()
            .parentClaimantIsApplicant(YesOrNo.NO)
            .helpWithFeesMoreInformationAdditional(HelpWithFeesMoreInformation.builder()
                                                       .hwFMoreInfoDocumentDate(NOW)
                                                       .hwFMoreInfoRequiredDocuments(
                                                           getMoreInformationDocumentList()).build())
            .additionalHwfDetails(hwfeeDetails).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_DEF);
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-more-info-bilingual-id",
            getNotificationDataMapMoreInfoAdditional(),
            REFERENCE_NUMBER
        );
    }

    private List<HwFMoreInfoRequiredDocuments> getMoreInformationDocumentList() {
        return Collections.singletonList(HwFMoreInfoRequiredDocuments.CHILD_MAINTENANCE);
    }

    private Map<String, String> getNotificationDataMapMoreInfoGa() {
        return Map.of(
                HWF_MORE_INFO_DATE, formatLocalDate(NOW, DATE),
                CLAIMANT_NAME, APPLICANT,
                CASE_REFERENCE, GA_REFERENCE,
                TYPE_OF_FEE, FeeType.APPLICATION.getLabel(),
                TYPE_OF_FEE_WELSH, FeeType.APPLICATION.getLabelInWelsh(),
                HWF_MORE_INFO_DOCUMENTS, getMoreInformationDocumentListString(),
                HWF_MORE_INFO_DOCUMENTS_WELSH, getMoreInformationDocumentListStringWelsh(),
                HWF_REFERENCE_NUMBER, HWF_REFERENCE
        );
    }

    private Map<String, String> getNotificationDataMapMoreInfoAdditional() {
        return Map.of(
                HWF_MORE_INFO_DATE, formatLocalDate(NOW, DATE),
                CLAIMANT_NAME, APPLICANT,
                CASE_REFERENCE, GA_REFERENCE,
                TYPE_OF_FEE, FeeType.ADDITIONAL.getLabel(),
                TYPE_OF_FEE_WELSH, FeeType.ADDITIONAL.getLabelInWelsh(),
                HWF_MORE_INFO_DOCUMENTS, getMoreInformationDocumentListString(),
                HWF_MORE_INFO_DOCUMENTS_WELSH, getMoreInformationDocumentListStringWelsh(),
                HWF_REFERENCE_NUMBER, HWF_REFERENCE
        );
    }

    private String getMoreInformationDocumentListString() {
        List<HwFMoreInfoRequiredDocuments> list = getMoreInformationDocumentList();
        StringBuilder documentList = new StringBuilder();
        for (HwFMoreInfoRequiredDocuments doc : list) {
            documentList.append(doc.getName());
            if (!doc.getDescription().isEmpty()) {
                documentList.append(" - ");
                documentList.append(doc.getDescription());
            }
            documentList.append("\n");
            documentList.append("\n");
        }
        return documentList.toString();
    }

    private String getMoreInformationDocumentListStringWelsh() {
        List<HwFMoreInfoRequiredDocuments> list = getMoreInformationDocumentList();
        StringBuilder documentList = new StringBuilder();
        for (HwFMoreInfoRequiredDocuments doc : list) {
            documentList.append(doc.getNameBilingual());
            if (!doc.getDescriptionBilingual().isEmpty()) {
                documentList.append(" - ");
                documentList.append(doc.getDescriptionBilingual());
            }
            documentList.append("\n");
            documentList.append("\n");
        }
        return documentList.toString();
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_RefNumberUpdated_ClaimIssued() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(UPDATE_HELP_WITH_FEE_NUMBER_GA)
                .build();
        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-updaterefnumber-id",
                getNotificationCommonDataMapForGa(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_RefNumberUpdated_Ga_Bilingual_Def() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(UPDATE_HELP_WITH_FEE_NUMBER_GA)
            .build();
        CaseData caseData = GA_CASE_DATA.toBuilder()
            .parentClaimantIsApplicant(YesOrNo.NO)
            .gaHwfDetails(hwfeeDetails).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_DEF);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-updaterefnumber-bilingual-id",
            getNotificationCommonDataMapForGa(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_RefNumberUpdated_Additional() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(UPDATE_HELP_WITH_FEE_NUMBER_GA)
                .build();
        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder().additionalHwfDetails(hwfeeDetails).parentCaseReference(GA_REFERENCE).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-updaterefnumber-id",
                getNotificationCommonDataMapForAdditional(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_RefNumberUpdated_Additional_Bilingual() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(UPDATE_HELP_WITH_FEE_NUMBER_GA)
            .build();
        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder().additionalHwfDetails(hwfeeDetails).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_CLA);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-updaterefnumber-bilingual-id",
            getNotificationCommonDataMapForAdditional(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_InvalidRefNumber_ClaimIssued() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(INVALID_HWF_REFERENCE_GA)
                .build();
        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-invalidrefnumber-id",
                getNotificationCommonDataMapForGa(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_InvalidRefNumber_ClaimIssued_Bilingual() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(INVALID_HWF_REFERENCE_GA)
            .build();
        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_CLA);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-invalidrefnumber-bilingual-id",
            getNotificationCommonDataMapForGa(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_InvalidRefNumber_Hearing() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(INVALID_HWF_REFERENCE_GA)
                .build();

        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder().additionalHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-invalidrefnumber-id",
                getNotificationCommonDataMapForAdditional(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_InvalidRefNumber_Hearing_Bilingual_Def() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(INVALID_HWF_REFERENCE_GA)
            .build();

        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder()
            .parentClaimantIsApplicant(YesOrNo.NO).additionalHwfDetails(hwfeeDetails).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_DEF);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-invalidrefnumber-bilingual-id",
            getNotificationCommonDataMapForAdditional(),
            REFERENCE_NUMBER
        );
    }

    private Map<String, String> getNotificationCommonDataMapForGa() {
        return Map.of(
                CLAIMANT_NAME, APPLICANT,
                CASE_REFERENCE, GA_REFERENCE,
                TYPE_OF_FEE, FeeType.APPLICATION.getLabel(),
                TYPE_OF_FEE_WELSH, FeeType.APPLICATION.getLabelInWelsh(),
                HWF_REFERENCE_NUMBER, HWF_REFERENCE
        );
    }

    private Map<String, String> getNotificationCommonDataMapForAdditional() {
        return Map.of(
                CLAIMANT_NAME, APPLICANT,
                CASE_REFERENCE, GA_REFERENCE,
                TYPE_OF_FEE, FeeType.ADDITIONAL.getLabel(),
                TYPE_OF_FEE_WELSH, FeeType.ADDITIONAL.getLabelInWelsh(),
                HWF_REFERENCE_NUMBER, HWF_REFERENCE
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_PartialRemission_Ga() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(PARTIAL_REMISSION_HWF_GA)
                .remissionAmount(new BigDecimal(REMISSION_AMOUNT))
                .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
                .build();

        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-partialRemission-id",
                getNotificationDataMapPartialRemissionGa(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_PartialRemission_Ga_Bilingual() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(PARTIAL_REMISSION_HWF_GA)
            .remissionAmount(new BigDecimal(REMISSION_AMOUNT))
            .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
            .build();

        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_CLA);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-partialRemission-bilingual-id",
            getNotificationDataMapPartialRemissionGa(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_PartialRemission_Additional() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(PARTIAL_REMISSION_HWF_GA)
                .remissionAmount(new BigDecimal(REMISSION_AMOUNT))
                .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
                .build();

        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder().additionalHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-partialRemission-id",
                getNotificationDataMapPartialRemissionAdditional(),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_PartialRemission_Additional_Bilingual_Def() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(PARTIAL_REMISSION_HWF_GA)
            .remissionAmount(new BigDecimal(REMISSION_AMOUNT))
            .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
            .build();

        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder()
            .parentClaimantIsApplicant(YesOrNo.NO)
            .additionalHwfDetails(hwfeeDetails).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_DEF);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-partialRemission-bilingual-id",
            getNotificationDataMapPartialRemissionAdditional(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfNoRemission_ClaimIssued() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(NO_REMISSION_HWF_GA)
            .noRemissionDetailsSummary(NoRemissionDetailsSummary.INCORRECT_EVIDENCE)
            .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
            .build();
        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails)
               .parentCaseReference(GA_REFERENCE).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-noRemission-id",
            getNotificationDataMapNoRemissionApplication(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfNoRemission_ClaimIssued_Bilingual() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(NO_REMISSION_HWF_GA)
            .noRemissionDetailsSummary(NoRemissionDetailsSummary.INCORRECT_EVIDENCE)
            .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
            .build();
        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_CLA);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-noremission-bilingual-id",
            getNotificationDataMapNoRemissionApplication(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfNoRemission_AdditionalFee() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(NO_REMISSION_HWF_GA)
            .noRemissionDetailsSummary(NoRemissionDetailsSummary.INCORRECT_EVIDENCE)
            .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
            .build();
        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder().additionalHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-noRemission-id",
            getNotificationDataMapNoRemissionAdditional(),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfNoRemission_AdditionalFee_Bilingual_Def() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(NO_REMISSION_HWF_GA)
            .noRemissionDetailsSummary(NoRemissionDetailsSummary.INCORRECT_EVIDENCE)
            .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
            .build();
        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder()
            .parentClaimantIsApplicant(YesOrNo.NO)
            .additionalHwfDetails(hwfeeDetails).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_DEF);
        // When
        service.sendNotification(caseData);
        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            "test-hwf-noremission-bilingual-id",
            getNotificationDataMapNoRemissionAdditional(),
            REFERENCE_NUMBER
        );
    }

    private Map<String, String> getNotificationDataMapNoRemissionApplication() {
        return Map.of(
            CLAIMANT_NAME, APPLICANT,
            CASE_REFERENCE, GA_REFERENCE,
            TYPE_OF_FEE, FeeType.APPLICATION.getLabel(),
            TYPE_OF_FEE_WELSH, FeeType.APPLICATION.getLabelInWelsh(),
            HWF_REFERENCE_NUMBER, HWF_REFERENCE,
            FEE_AMOUNT, OUTSTANDING_AMOUNT_IN_POUNDS,
            NO_REMISSION_REASONS, NoRemissionDetailsSummary.INCORRECT_EVIDENCE.getLabel(),
            NO_REMISSION_REASONS_WELSH, NoRemissionDetailsSummary.INCORRECT_EVIDENCE.getLabelWelsh()
        );
    }

    private Map<String, String> getNotificationDataMapNoRemissionAdditional() {
        return Map.of(
            CLAIMANT_NAME, APPLICANT,
            CASE_REFERENCE, GA_REFERENCE,
            TYPE_OF_FEE, FeeType.ADDITIONAL.getLabel(),
            TYPE_OF_FEE_WELSH, FeeType.ADDITIONAL.getLabelInWelsh(),
            HWF_REFERENCE_NUMBER, HWF_REFERENCE,
            FEE_AMOUNT, OUTSTANDING_AMOUNT_IN_POUNDS,
            NO_REMISSION_REASONS, NoRemissionDetailsSummary.INCORRECT_EVIDENCE.getLabel(),
            NO_REMISSION_REASONS_WELSH, NoRemissionDetailsSummary.INCORRECT_EVIDENCE.getLabelWelsh()
        );
    }

    private Map<String, String> getNotificationDataMapPartialRemissionGa() {
        return Map.of(
                CLAIMANT_NAME, APPLICANT,
                CASE_REFERENCE, GA_REFERENCE,
                TYPE_OF_FEE, FeeType.APPLICATION.getLabel(),
                TYPE_OF_FEE_WELSH, FeeType.APPLICATION.getLabelInWelsh(),
                HWF_REFERENCE_NUMBER, HWF_REFERENCE,
                PART_AMOUNT, "1000.00",
                REMAINING_AMOUNT, OUTSTANDING_AMOUNT_IN_POUNDS
        );
    }

    private Map<String, String> getNotificationDataMapPartialRemissionAdditional() {
        return Map.of(
                CLAIMANT_NAME, APPLICANT,
                CASE_REFERENCE, GA_REFERENCE,
                TYPE_OF_FEE, FeeType.ADDITIONAL.getLabel(),
                TYPE_OF_FEE_WELSH, FeeType.ADDITIONAL.getLabelInWelsh(),
                HWF_REFERENCE_NUMBER, HWF_REFERENCE,
                PART_AMOUNT, "1000.00",
                REMAINING_AMOUNT, OUTSTANDING_AMOUNT_IN_POUNDS
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_PaymentOut_Ga() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(FEE_PAYMENT_OUTCOME_GA)
                .remissionAmount(new BigDecimal(REMISSION_AMOUNT))
                .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
                .build();

        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-paymentoutcome-id",
                getNotificationDataMapPaymentOutcome(FeeType.APPLICATION),
                REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_PaymentOut_Ga_inWelsh() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
            .hwfCaseEvent(FEE_PAYMENT_OUTCOME_GA)
            .remissionAmount(new BigDecimal(REMISSION_AMOUNT))
            .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
            .build();

        CaseData caseData = GA_CASE_DATA.toBuilder().gaHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CIVIL_WELSH_CLA);
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
            EMAIL,
            EMAIL_TEMPLATE_HWF_PAYMENT_OUTCOME_WLESH,
            getNotificationDataMapPaymentOutcome(FeeType.APPLICATION),
            REFERENCE_NUMBER
        );
    }

    @Test
    void shouldNotifyApplicant_HwfOutcome_PaymentOut_Additional() {
        // Given
        HelpWithFeesDetails hwfeeDetails = HelpWithFeesDetails.builder()
                .hwfCaseEvent(FEE_PAYMENT_OUTCOME_GA)
                .remissionAmount(new BigDecimal(REMISSION_AMOUNT))
                .outstandingFeeInPounds(new BigDecimal(OUTSTANDING_AMOUNT_IN_POUNDS))
                .build();

        CaseData caseData = ADDITIONAL_CASE_DATA.toBuilder().additionalHwfDetails(hwfeeDetails)
            .parentCaseReference(GA_REFERENCE).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                "test-hwf-paymentoutcome-id",
                getNotificationDataMapPaymentOutcome(FeeType.ADDITIONAL),
                REFERENCE_NUMBER
        );
    }

    private Map<String, String> getNotificationDataMapPaymentOutcome(FeeType feeType) {
        return Map.of(
            CLAIMANT_NAME, APPLICANT,
            CASE_REFERENCE, GA_REFERENCE,
            TYPE_OF_FEE, feeType.getLabel(),
            TYPE_OF_FEE_WELSH, feeType.getLabelInWelsh(),
            HWF_REFERENCE_NUMBER, HWF_REFERENCE,
            CASE_TITLE, "Mr. John Rambo v Mr. Joe Doe",
            APPLICANT_NAME, "Mr. John Rambo"
        );
    }

}
