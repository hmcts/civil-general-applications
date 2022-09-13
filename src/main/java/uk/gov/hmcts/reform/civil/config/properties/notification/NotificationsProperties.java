package uk.gov.hmcts.reform.civil.config.properties.notification;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@Data
public class NotificationsProperties {

    @NotEmpty
    private String govNotifyApiKey;

    @NotEmpty
    private String generalApplicationRespondentEmailTemplate;

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
    private String judgeUncloakApplicationEmailTemplate;

    @NotEmpty
    private String judgeApproveOrderToStrikeOutDamages;

    @NotEmpty
    private String judgeApproveOrderToStrikeOutOCMC;

    @NotEmpty
    private String generalApplicationPaymentFailure;
}
