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
    private String respondentWrittenRepConcurrentRepresentationEmailTemplate;

    @NotEmpty
    private String applicantWrittenRepConcurrentRepresentationEmailTemplate;

    @NotEmpty
    private String respondentWrittenRepSequentialRepresentationEmailTemplate;

    @NotEmpty
    private String applicantWrittenRepSequentialRepresentationEmailTemplate;

    @NotEmpty
    private String judgeDismissesOrderApplicantEmailTemplate;

    @NotEmpty
    private String judgeListsForHearingApplicantEmailTemplate;

    @NotEmpty
    private String judgeUncloaksApplicationForDismissedCaseApplicantEmailTemplate;

    @NotEmpty
    private String judgeUncloaksApplicationForApprovedCaseApplicantEmailTemplate;

    @NotEmpty
    private String judgeHasOrderedTheApplicationApprovedEmailTemplate;

    @NotEmpty
    private String judgeListsForHearingRespondentEmailTemplate;

    @NotEmpty
    private String judgeDismissesOrderRespondentEmailTemplate;
}
