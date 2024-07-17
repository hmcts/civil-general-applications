package uk.gov.hmcts.reform.civil.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MORE_INFORMATION_HWF_GA;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_APPLICATION_PAYMENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.CASE_REFERENCE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.CLAIMANT_NAME;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HWF_MORE_INFO_DATE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HWF_MORE_INFO_DOCUMENTS;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HWF_REFERENCE_NUMBER;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.TYPE_OF_FEE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.formatLocalDate;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.enums.HwFMoreInfoRequiredDocuments;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
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

@ExtendWith(MockitoExtension.class)
public class HwfNotificationServiceTest {

    private static final String EMAIL_TEMPLATE_MORE_INFO_HWF = "test-hwf-more-info-id";
    private static final String EMAIL = "test@email.com";
    private static final String APPLICANT = "Mr. John Rambo";
    private static final String GA_REFERENCE = "1111222233334444";
    private static final String HWF_REFERENCE = "000HWF001";
    private static final String REFERENCE_NUMBER = "1";
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
            .ccdCaseReference(1111222233334444L)
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                    .email(EMAIL)
                    .build()).build()
            .toBuilder()
            .applicantPartyName(APPLICANT)
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
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                    .email(EMAIL)
                    .build()).build()
            .toBuilder()
            .applicantPartyName(APPLICANT)
            .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber(
                    HWF_REFERENCE).build())
            .generalAppPBADetails(GAPbaDetails.builder()
                    .fee(Fee.builder().calculatedAmountInPence(BigDecimal.valueOf(100000)).build())
                    .build())
            .hwfFeeType(FeeType.ADDITIONAL)
            .build();

    @BeforeEach
    void setup() {
        when(coreCaseDataService.getCase(any())).thenReturn(CaseDetails.builder().build());
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
        when(notificationsProperties.getNotifyApplicantForHwFMoreInformationNeeded()).thenReturn(
                EMAIL_TEMPLATE_MORE_INFO_HWF);
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
                .gaHwfDetails(hwfeeDetails).build();
        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                EMAIL_TEMPLATE_MORE_INFO_HWF,
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
                .additionalHwfDetails(hwfeeDetails).build();

        when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);

        // When
        service.sendNotification(caseData);

        // Then
        verify(notificationService, times(1)).sendMail(
                EMAIL,
                EMAIL_TEMPLATE_MORE_INFO_HWF,
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
                HWF_MORE_INFO_DOCUMENTS, getMoreInformationDocumentListString(),
                HWF_REFERENCE_NUMBER, HWF_REFERENCE
        );
    }

    private Map<String, String> getNotificationDataMapMoreInfoAdditional() {
        return Map.of(
                HWF_MORE_INFO_DATE, formatLocalDate(NOW, DATE),
                CLAIMANT_NAME, APPLICANT,
                CASE_REFERENCE, GA_REFERENCE,
                TYPE_OF_FEE, FeeType.ADDITIONAL.getLabel(),
                HWF_MORE_INFO_DOCUMENTS, getMoreInformationDocumentListString(),
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

}
