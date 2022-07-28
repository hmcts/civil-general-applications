package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.JUDICIAL_FORMATTER;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.areRespondentSolicitorsPresent;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.notificationCriterion;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.requiredGAType;

@Service
@RequiredArgsConstructor
public class JudicialNotificationService implements NotificationData {

    private final ObjectMapper objectMapper;
    private final NotificationsProperties notificationProperties;
    private final NotificationService notificationService;
    private final Map<String, String> customProps;
    private static final String REFERENCE_TEMPLATE = "general-apps-judicial-notification-make-decision-%s";

    public void sendNotification(CaseData caseData) throws NotificationException {
        switch (notificationCriterion(caseData)) {
            case CONCURRENT_WRITTEN_REP:
                concurrentWrittenRepNotification(caseData);
                break;
            case SEQUENTIAL_WRITTEN_REP:
                sequentialWrittenRepNotification(caseData);
                break;
            case LIST_FOR_HEARING:
                applicationListForHearing(caseData);
                break;
            case JUDGE_APPROVED_THE_ORDER:
                applicationApprovedNotification(caseData);
                break;
            case JUDGE_APPROVED_THE_ORDER_CLOAK:
                judgeApprovedOrderApplicationCloak(caseData);
                break;
            case JUDGE_DISMISSED_APPLICATION:
                applicationDismissedByJudge(caseData);
                break;
            case JUDGE_DISMISSED_APPLICATION_CLOAK:
                judgeDismissedOrderApplicationCloak(caseData);
                break;
            case JUDGE_DIRECTION_ORDER:
                applicationDirectionOrder(caseData);
                break;
            case JUDGE_DIRECTION_ORDER_CLOAK:
                applicationDirectionOrderCloak(caseData);
                break;
            case REQUEST_FOR_INFORMATION:
                applicationRequestForInformation(caseData);
                break;
            default:
            case NON_CRITERION:
        }

    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        customProps.put(
            CASE_REFERENCE,
            Objects.requireNonNull(caseData.getGeneralAppParentCaseLink().getCaseReference())
        );
        customProps.put(
            GA_APPLICATION_TYPE,
            Objects.requireNonNull(requiredGAType(caseData))
        );
        return customProps;
    }

    private void sendNotificationForJudicialDecision(CaseData caseData, String recipient, String template)
        throws NotificationException {
        try {
            notificationService.sendMail(recipient, template, addProperties(caseData),
                                         String.format(REFERENCE_TEMPLATE,
                                                       caseData.getGeneralAppParentCaseLink().getCaseReference()));
        } catch (NotificationException e) {
            throw new NotificationException(e);
        }
    }

    private void concurrentWrittenRepNotification(CaseData caseData) {
        var concurrentDateText = Optional.ofNullable(caseData
                                                         .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                                         .getWrittenConcurrentRepresentationsBy()).orElse(null);
        customProps.put(
            GA_JUDICIAL_CONCURRENT_DATE_TEXT,
            Objects.nonNull(concurrentDateText)
                ? DateFormatHelper
                .formatLocalDate(
                    LocalDate.parse(
                        concurrentDateText.toString(),
                        JUDICIAL_FORMATTER
                    ), DATE) : null
        );
        if (areRespondentSolicitorsPresent(caseData)) {
            caseData.getGeneralAppRespondentSolicitors().forEach(
                respondentSolicitor -> sendNotificationForJudicialDecision(
                    caseData, respondentSolicitor.getValue().getEmail(),
                    notificationProperties.getWrittenRepConcurrentRepresentationRespondentEmailTemplate()
                ));
        }

        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getWrittenRepConcurrentRepresentationApplicantEmailTemplate()
        );
        customProps.remove(GA_JUDICIAL_CONCURRENT_DATE_TEXT);
    }

    private void sequentialWrittenRepNotification(CaseData caseData) {

        var sequentialDateTextRespondent = Optional
            .ofNullable(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                            .getSequentialApplicantMustRespondWithin()).orElse(null);

        customProps.put(
            GA_JUDICIAL_SEQUENTIAL_DATE_TEXT_RESPONDENT,
            Objects.nonNull(sequentialDateTextRespondent)
                ? DateFormatHelper
                .formatLocalDate(
                    LocalDate.parse(
                        sequentialDateTextRespondent.toString(),
                        JUDICIAL_FORMATTER
                    ), DATE) : null
        );

        if (areRespondentSolicitorsPresent(caseData)) {
            caseData.getGeneralAppRespondentSolicitors().forEach(
                respondentSolicitor -> sendNotificationForJudicialDecision(
                    caseData, respondentSolicitor.getValue().getEmail(),
                    notificationProperties.getWrittenRepSequentialRepresentationRespondentEmailTemplate()
                ));
        }
        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getWrittenRepSequentialRepresentationApplicantEmailTemplate()
        );

        customProps.remove(GA_JUDICIAL_SEQUENTIAL_DATE_TEXT_RESPONDENT);
    }

    private void applicationRequestForInformation(CaseData caseData) {

        var requestForInformationDeadline = Optional.ofNullable(caseData
                                                                    .getJudicialDecisionRequestMoreInfo()
                                                                    .getJudgeRequestMoreInfoByDate()).orElse(null);

        customProps.put(
            GA_REQUEST_FOR_INFORMATION_DEADLINE,
            Objects.nonNull(requestForInformationDeadline)
                ? DateFormatHelper
                .formatLocalDate(
                    LocalDate.parse(
                        requestForInformationDeadline.toString(),
                        JUDICIAL_FORMATTER
                    ), DATE) : null
        );

        if (areRespondentSolicitorsPresent(caseData)) {
            caseData.getGeneralAppRespondentSolicitors().forEach(
                respondentSolicitor -> sendNotificationForJudicialDecision(
                    caseData, respondentSolicitor.getValue().getEmail(),
                    notificationProperties.getJudgeRequestForInformationRespondentEmailTemplate()
                ));
        }
        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeRequestForInformationApplicantEmailTemplate()
        );

        customProps.remove(GA_REQUEST_FOR_INFORMATION_DEADLINE);
    }

    private void applicationApprovedNotification(CaseData caseData) {
        if (isSendUncloakAdditionalFeeEmail(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getJudgeUncloakApplicationEmailTemplate()
            );
        } else {
            if (areRespondentSolicitorsPresent(caseData)) {
                caseData.getGeneralAppRespondentSolicitors().forEach(
                    respondentSolicitor -> sendNotificationForJudicialDecision(
                        caseData, respondentSolicitor.getValue().getEmail(),
                        notificationProperties.getJudgeForApproveRespondentEmailTemplate()
                    ));
            }
            if (useApplicantTemplate(caseData)) {
                sendNotificationForJudicialDecision(
                    caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getJudgeForApprovedCaseApplicantEmailTemplate()
                );
            }
        }

    }

    private void applicationListForHearing(CaseData caseData) {
        if (areRespondentSolicitorsPresent(caseData)) {
            caseData.getGeneralAppRespondentSolicitors().forEach(
                respondentSolicitor -> sendNotificationForJudicialDecision(
                    caseData, respondentSolicitor.getValue().getEmail(),
                    notificationProperties.getJudgeListsForHearingRespondentEmailTemplate()
                ));
        }

        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeListsForHearingApplicantEmailTemplate()
        );
    }

    private void applicationDismissedByJudge(CaseData caseData) {
        if (isSendUncloakAdditionalFeeEmail(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getJudgeUncloakApplicationEmailTemplate()
            );
        } else {
            if (areRespondentSolicitorsPresent(caseData)) {
                caseData.getGeneralAppRespondentSolicitors().forEach(
                    respondentSolicitor -> sendNotificationForJudicialDecision(
                        caseData, respondentSolicitor.getValue().getEmail(),
                        notificationProperties.getJudgeDismissesOrderRespondentEmailTemplate()
                    ));
            }
            if (useApplicantTemplate(caseData)) {
                sendNotificationForJudicialDecision(
                    caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getJudgeDismissesOrderApplicantEmailTemplate()
                );
            }
        }
    }

    private void applicationDirectionOrder(CaseData caseData) {
        if (isSendUncloakAdditionalFeeEmail(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getJudgeUncloakApplicationEmailTemplate()
            );
        } else {
            if (areRespondentSolicitorsPresent(caseData)) {
                caseData.getGeneralAppRespondentSolicitors().forEach(
                    respondentSolicitor -> sendNotificationForJudicialDecision(
                        caseData, respondentSolicitor.getValue().getEmail(),
                        notificationProperties.getJudgeForDirectionOrderRespondentEmailTemplate()
                    ));
            }
            if (useApplicantTemplate(caseData)) {
                sendNotificationForJudicialDecision(
                    caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getJudgeForDirectionOrderApplicantEmailTemplate()
                );
            }
        }
    }

    private void judgeApprovedOrderApplicationCloak(CaseData caseData) {
        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeForApprovedCaseApplicantEmailTemplate()
        );
    }

    private void judgeDismissedOrderApplicationCloak(CaseData caseData) {
        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeDismissesOrderApplicantEmailTemplate()
        );
    }

    private void applicationDirectionOrderCloak(CaseData caseData) {
        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeForDirectionOrderApplicantEmailTemplate()
        );
    }

    public static boolean useApplicantTemplate(CaseData caseData) {
        return caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES)
            || caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(YES);
    }

    public static boolean isSendUncloakAdditionalFeeEmail(CaseData caseData) {
        return caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(NO)
            && caseData.getGeneralAppPBADetails().getAdditionalPaymentDetails() == null;
    }
}
