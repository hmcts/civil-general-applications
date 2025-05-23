package uk.gov.hmcts.reform.civil.config.properties.notification;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class NotificationsProperties {

    @NotEmpty
    private String govNotifyApiKey;

    @NotEmpty
    private String generalApplicationRespondentEmailTemplate;

    @NotEmpty
    private String urgentGeneralAppRespondentEmailTemplate;

    @NotEmpty
    private String lipGeneralAppRespondentEmailTemplate;

    @NotEmpty
    private String lipGeneralAppRespondentEmailTemplateInWelsh;

    @NotEmpty
    private String lipGeneralAppApplicantEmailTemplate;

    @NotEmpty
    private String lipGeneralAppApplicantEmailTemplateInWelsh;

    @NotEmpty
    private String withNoticeUpdateRespondentEmailTemplate;

    @NotEmpty
    private String writtenRepConcurrentRepresentationApplicantEmailTemplate;

    @NotEmpty
    private String writtenRepConcurrentRepresentationRespondentEmailTemplate;

    @NotEmpty
    private String writtenRepSequentialRepresentationApplicantEmailTemplate;

    @NotEmpty
    private String writtenRepSequentialRepresentationRespondentEmailTemplate;

    @NotEmpty
    private String judgeListsForHearingApplicantEmailTemplate;

    @NotEmpty
    private String judgeListsForHearingRespondentEmailTemplate;

    @NotEmpty
    private String judgeForApprovedCaseApplicantEmailTemplate;

    @NotEmpty
    private String judgeForApproveRespondentEmailTemplate;

    @NotEmpty
    private String judgeDismissesOrderApplicantEmailTemplate;

    @NotEmpty
    private String judgeDismissesOrderRespondentEmailTemplate;

    @NotEmpty
    private String judgeForDirectionOrderApplicantEmailTemplate;

    @NotEmpty
    private String judgeForDirectionOrderRespondentEmailTemplate;

    @NotEmpty
    private String judgeRequestForInformationApplicantEmailTemplate;

    @NotEmpty
    private String judgeRequestForInformationRespondentEmailTemplate;

    @NotEmpty
    private String judgeFreeFormOrderApplicantEmailTemplate;

    @NotEmpty
    private String judgeFreeFormOrderRespondentEmailTemplate;

    @NotEmpty
    private String judgeUncloakApplicationEmailTemplate;

    @NotEmpty
    private String judgeApproveOrderToStrikeOutDamages;

    @NotEmpty
    private String judgeApproveOrderToStrikeOutOCMC;

    @NotEmpty
    private String hearingNoticeTemplate;

    @NotEmpty
    private String evidenceUploadTemplate;

    @NotEmpty
    private String notifyApplicantForHwFMoreInformationNeeded;

    @NotEmpty
    private String notifyApplicantForHwfInvalidRefNumber;

    @NotEmpty
    private String notifyApplicantForHwfUpdateRefNumber;

    @NotEmpty
    private String notifyApplicantForHwfPartialRemission;
    @NotEmpty
    private String notifyApplicantForNoRemission;
    @NotEmpty
    private String notifyApplicantForHwfPaymentOutcome;
    @NotEmpty
    private String notifyApplicantLiPTranslatedDocumentUploadedWhenParentCaseInBilingual;
    @NotEmpty
    private String notifyRespondentLiPTranslatedDocumentUploadedWhenParentCaseInBilingual;
    @NotEmpty
    private String notifyLRTranslatedDocumentUploaded;

    @NotEmpty
    private String notifyApplicantForHwfInvalidRefNumberBilingual;
    @NotEmpty
    private String notifyApplicantForHwFMoreInformationNeededWelsh;
    @NotEmpty
    private String notifyApplicantForHwfNoRemissionWelsh;
    @NotEmpty
    private String notifyApplicantForHwfUpdateRefNumberBilingual;
    @NotEmpty
    private String notifyApplicantForHwfPartialRemissionBilingual;

}
