package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STRIKE_OUT;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.JUDICIAL_FORMATTER;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.areRespondentSolicitorsPresent;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isApplicationCloaked;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isGeneralAppConsentOrder;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isWithNotice;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.notificationCriterion;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.requiredGAType;

@Service
@RequiredArgsConstructor
public class JudicialNotificationService implements NotificationData {

    private static final String RESPONDENT = "respondent";
    private static final String APPLICANT = "applicant";

    private final NotificationsProperties notificationProperties;
    private final NotificationService notificationService;
    private final Map<String, String> customProps;
    private static final String REFERENCE_TEMPLATE = "general-apps-judicial-notification-make-decision-%s";

    private final DeadlinesCalculator deadlinesCalculator;
    private static final int NUMBER_OF_DEADLINE_DAYS = 5;
    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;

    private final SolicitorEmailValidation solicitorEmailValidation;
    private final JudicialDecisionHelper judicialDecisionHelper;

    public CaseData sendNotification(CaseData caseData, String solicitorType) throws NotificationException {
        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        caseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);

        switch (notificationCriterion(caseData)) {
            case CONCURRENT_WRITTEN_REP:
                concurrentWrittenRepNotification(caseData, solicitorType);
                break;
            case SEQUENTIAL_WRITTEN_REP:
                sequentialWrittenRepNotification(caseData, solicitorType);
                break;
            case LIST_FOR_HEARING:
                applicationListForHearing(caseData, solicitorType);
                break;
            case JUDGE_APPROVED_THE_ORDER:
                applicationApprovedNotification(caseData, solicitorType);
                break;
            case JUDGE_APPROVED_THE_ORDER_CLOAK:
                judgeApprovedOrderApplicationCloak(caseData, solicitorType);
                break;
            case JUDGE_DISMISSED_APPLICATION:
                applicationDismissedByJudge(caseData, solicitorType);
                break;
            case JUDGE_DISMISSED_APPLICATION_CLOAK:
                judgeDismissedOrderApplicationCloak(caseData, solicitorType);
                break;
            case JUDGE_DIRECTION_ORDER:
                applicationDirectionOrder(caseData, solicitorType);
                break;
            case JUDGE_DIRECTION_ORDER_CLOAK:
                applicationDirectionOrderCloak(caseData, solicitorType);
                break;
            case REQUEST_FOR_INFORMATION:
                caseData = applicationRequestForInformation(caseData, solicitorType);
                break;
            case REQUEST_FOR_INFORMATION_CLOAK:
                applicationRequestForInformationCloak(caseData, solicitorType);
                break;
            default:
            case NON_CRITERION:
        }
        return caseData;
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

    private void concurrentWrittenRepNotification(CaseData caseData, String solicitorType) {
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

        if (solicitorType.equals(RESPONDENT) && areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                    caseData,
                    notificationProperties.getWrittenRepConcurrentRepresentationRespondentEmailTemplate()
            );
        }

        if (solicitorType.equals(APPLICANT)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getWrittenRepConcurrentRepresentationApplicantEmailTemplate()
            );
        }

        customProps.remove(GA_JUDICIAL_CONCURRENT_DATE_TEXT);
    }

    private void sequentialWrittenRepNotification(CaseData caseData, String solicitorType) {

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

        if (solicitorType.equals(RESPONDENT)
            && areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                    caseData,
                    notificationProperties.getWrittenRepSequentialRepresentationRespondentEmailTemplate()
            );
        }

        if (solicitorType.equals(APPLICANT)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getWrittenRepSequentialRepresentationApplicantEmailTemplate()
            );
        }

        customProps.remove(GA_JUDICIAL_SEQUENTIAL_DATE_TEXT_RESPONDENT);
    }

    private CaseData applicationRequestForInformation(CaseData caseData, String solicitorType) {

        if (solicitorType.equals(RESPONDENT)
            && (caseData.getCcdState().equals(CaseState.APPLICATION_ADD_PAYMENT)
                || judicialDecisionHelper.containsTypesNeedNoAdditionalFee(caseData))) {

            // Send notification to respondent if payment is made
            caseData = addDeadlineForMoreInformationUncloakedApplication(caseData);
            var requestForInformationDeadline = caseData.getGeneralAppNotificationDeadlineDate();

            customProps.put(
                GA_NOTIFICATION_DEADLINE,
                Objects.nonNull(requestForInformationDeadline)
                    ? DateFormatHelper
                    .formatLocalDateTime(requestForInformationDeadline, DATE) : null);

            if (areRespondentSolicitorsPresent(caseData)) {
                sendEmailToRespondent(
                    caseData,
                    notificationProperties.getGeneralApplicationRespondentEmailTemplate()
                );
            }
            customProps.remove(GA_NOTIFICATION_DEADLINE);

        }

        if ((isSendUncloakAdditionalFeeEmailForWithoutNotice(caseData)
            || isSendUncloakAdditionalFeeEmailConsentOrder(caseData))) {
            // Send notification to applicant only if it's without notice application
            if (solicitorType.equals(APPLICANT)
                    && !judicialDecisionHelper.containsTypesNeedNoAdditionalFee(caseData)) {
                String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();

                sendNotificationForJudicialDecision(
                    caseData,
                    appSolicitorEmail,
                    notificationProperties.getJudgeUncloakApplicationEmailTemplate()
                );
            }
        } else {
            // send notification to applicant and respondent if it's with notice application
            sendToBoth(caseData, solicitorType);
        }
        return caseData;
    }

    private void sendToBoth(CaseData caseData, String solicitorType) {
        addCustomPropsForRespondDeadline(caseData.getJudicialDecisionRequestMoreInfo()
                .getJudgeRequestMoreInfoByDate());

        if (solicitorType.equals(RESPONDENT) && areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                    caseData,
                    notificationProperties.getJudgeRequestForInformationRespondentEmailTemplate()
            );
        }

        if (solicitorType.equals(APPLICANT)) {
            sendNotificationForJudicialDecision(
                    caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getJudgeRequestForInformationApplicantEmailTemplate()
            );
        }
        customProps.remove(GA_REQUEST_FOR_INFORMATION_DEADLINE);
    }

    private CaseData applicationRequestForInformationCloak(CaseData caseData, String solicitorType) {

        if (solicitorType.equals(APPLICANT)) {
            addCustomPropsForRespondDeadline(caseData.getJudicialDecisionRequestMoreInfo()
                                                 .getJudgeRequestMoreInfoByDate());
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getJudgeRequestForInformationApplicantEmailTemplate()
            );

            customProps.remove(GA_REQUEST_FOR_INFORMATION_DEADLINE);
        }

        return caseData;
    }

    private void applicationApprovedNotification(CaseData caseData, String solicitorType) {

        if (solicitorType.equals(RESPONDENT)) {
            boolean sendEmailToDefendant = isSendEmailToDefendant(caseData);

            if (sendEmailToDefendant) {
                if (useDamageTemplate(caseData)) {
                    sendEmailToRespondent(
                        caseData,
                        notificationProperties.getJudgeApproveOrderToStrikeOutDamages()
                    );
                } else if (useOcmcTemplate(caseData)) {
                    sendEmailToRespondent(
                        caseData,
                        notificationProperties.getJudgeApproveOrderToStrikeOutOCMC()
                    );
                } else {
                    sendEmailToRespondent(
                        caseData,
                        notificationProperties.getJudgeForApproveRespondentEmailTemplate()
                    );
                }
            }
        }

        if (solicitorType.equals(APPLICANT)) {
            String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();

            if (useDamageTemplate(caseData)) {
                sendNotificationForJudicialDecision(
                    caseData,
                    appSolicitorEmail,
                    notificationProperties.getJudgeApproveOrderToStrikeOutDamages()
                );
            } else if (useOcmcTemplate(caseData)) {
                sendNotificationForJudicialDecision(
                    caseData,
                    appSolicitorEmail,
                    notificationProperties.getJudgeApproveOrderToStrikeOutOCMC()
                );
            } else {
                sendNotificationForJudicialDecision(
                    caseData,
                    appSolicitorEmail,
                    notificationProperties.getJudgeForApprovedCaseApplicantEmailTemplate()
                );
            }
        }

    }

    private boolean isSendEmailToDefendant(CaseData caseData) {
        return areRespondentSolicitorsPresent(caseData)
            && (!isApplicationCloaked(caseData) || isGeneralAppConsentOrder(caseData));
    }

    private void applicationListForHearing(CaseData caseData, String solicitorType) {

        if (solicitorType.equals(RESPONDENT)) {
            /*
            * Respondent should receive notification only if it's with notice application
            *  */
            if (isWithNotice(caseData) && areRespondentSolicitorsPresent(caseData)) {
                sendEmailToRespondent(
                    caseData,
                    notificationProperties.getJudgeListsForHearingRespondentEmailTemplate()
                );
            }
        }

        if (solicitorType.equals(APPLICANT)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getJudgeListsForHearingApplicantEmailTemplate()
            );
        }
    }

    private void applicationDismissedByJudge(CaseData caseData, String solicitorType) {

        if (solicitorType.equals(RESPONDENT)
            && isSendEmailToDefendant(caseData)) {
            sendEmailToRespondent(
                    caseData,
                    notificationProperties.getJudgeDismissesOrderRespondentEmailTemplate()
            );
        }

        if (solicitorType.equals(APPLICANT)) {
            String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();

            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeDismissesOrderApplicantEmailTemplate()
            );
        }
    }

    private void applicationDirectionOrder(CaseData caseData, String solicitorType) {
        if (solicitorType.equals(RESPONDENT)
            && isSendEmailToDefendant(caseData)) {
            sendEmailToRespondent(
                    caseData,
                    notificationProperties.getJudgeForDirectionOrderRespondentEmailTemplate()
            );
        }

        if (solicitorType.equals(APPLICANT)) {
            String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();

            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeForDirectionOrderApplicantEmailTemplate()
            );
        }
    }

    private void judgeApprovedOrderApplicationCloak(CaseData caseData, String solicitorType) {

        if (solicitorType.equals(APPLICANT)) {
            String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();
            if (useDamageTemplate(caseData)) {
                sendNotificationForJudicialDecision(caseData,
                                                    appSolicitorEmail,
                                                    notificationProperties.getJudgeApproveOrderToStrikeOutDamages());
            } else if (useOcmcTemplate(caseData)) {
                sendNotificationForJudicialDecision(caseData,
                                                    appSolicitorEmail,
                                                    notificationProperties.getJudgeApproveOrderToStrikeOutOCMC());
            } else {
                sendNotificationForJudicialDecision(caseData,
                                                    appSolicitorEmail,
                                                    notificationProperties
                                                        .getJudgeForApprovedCaseApplicantEmailTemplate());
            }
        }
    }

    private void judgeDismissedOrderApplicationCloak(CaseData caseData, String solicitorType) {
        if (solicitorType.equals(APPLICANT)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getJudgeDismissesOrderApplicantEmailTemplate()
            );
        }
    }

    private void applicationDirectionOrderCloak(CaseData caseData, String solicitorType) {
        if (solicitorType.equals(APPLICANT)) {
            sendNotificationForJudicialDecision(
                caseData,
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getJudgeForDirectionOrderApplicantEmailTemplate()
            );
        }
    }

    private void sendEmailToRespondent(CaseData caseData, String notificationProperties) {
        caseData.getGeneralAppRespondentSolicitors().forEach(
            respondentSolicitor -> sendNotificationForJudicialDecision(caseData,
                                                                       respondentSolicitor.getValue().getEmail(),
                                                                       notificationProperties
            ));
    }

    private boolean isSendUncloakAdditionalFeeEmailForWithoutNotice(CaseData caseData) {
        return caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(NO)
            && caseData.getGeneralAppPBADetails().getAdditionalPaymentDetails() == null;
    }

    private boolean isSendUncloakAdditionalFeeEmailConsentOrder(CaseData caseData) {
        return isGeneralAppConsentOrder(caseData)
            && SEND_APP_TO_OTHER_PARTY.equals(caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption())
            && caseData.getGeneralAppPBADetails().getAdditionalPaymentDetails() == null;
    }

    private  void addCustomPropsForRespondDeadline(LocalDate requestForInformationDeadline) {
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
    }

    private CaseData addDeadlineForMoreInformationUncloakedApplication(CaseData caseData) {

        GAJudicialRequestMoreInfo judicialRequestMoreInfo = caseData.getJudicialDecisionRequestMoreInfo();

        if (SEND_APP_TO_OTHER_PARTY.equals(judicialRequestMoreInfo.getRequestMoreInfoOption())) {

            LocalDateTime deadlineForMoreInfoSubmission = deadlinesCalculator
                .calculateApplicantResponseDeadline(
                    LocalDateTime.now(), NUMBER_OF_DEADLINE_DAYS);

            caseData = caseData.toBuilder()
                .generalAppNotificationDeadlineDate(deadlineForMoreInfoSubmission)
                .build();
        }

        return caseData;
    }

    public static boolean useDamageTemplate(CaseData caseData) {
        return caseData.getGeneralAppType().getTypes().contains(STRIKE_OUT)
            && caseData.getGeneralAppSuperClaimType().equals("UNSPEC_CLAIM");
    }

    public static boolean useOcmcTemplate(CaseData caseData) {
        return caseData.getGeneralAppType().getTypes().contains(STRIKE_OUT)
            && caseData.getGeneralAppSuperClaimType().equals("SPEC_CLAIM");
    }

}
