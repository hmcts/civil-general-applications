package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.service.SolicitorEmailValidation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_NOTIFICATION_PROCESS_MAKE_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STRIKE_OUT;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.JUDICIAL_FORMATTER;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.areRespondentSolicitorsPresent;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.notificationCriterion;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.requiredGAType;

@Service
@RequiredArgsConstructor
public class JudicialDecisionNotificationHandler extends CallbackHandler implements NotificationData {

    private final ObjectMapper objectMapper;
    private final NotificationsProperties notificationProperties;
    private final NotificationService notificationService;
    private final Map<String, String> customProps;

    private static final List<CaseEvent> EVENTS = List.of(
        START_NOTIFICATION_PROCESS_MAKE_DECISION
    );

    private static final String REFERENCE_TEMPLATE = "general-apps-judicial-notification-make-decision-%s";

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;

    private final SolicitorEmailValidation solicitorEmailValidation;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::judicialDecisionNotification
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse judicialDecisionNotification(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        caseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);

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
            case  JUDGE_APPROVED_THE_ORDER_CLOAK:
                judgeApprovedOrderApplicationCloak(caseData);
                break;
            case JUDGE_DISMISSED_APPLICATION:
                applicationDismissedByJudge(caseData);
                break;
            case  JUDGE_DISMISSED_APPLICATION_CLOAK:
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
            default:case NON_CRITERION:
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }

    private void sendNotificationForJudicialDecision(CaseData caseData, String recipient, String template) {
        notificationService.sendMail(
            recipient,
            template,
            addProperties(caseData),
            String.format(REFERENCE_TEMPLATE, caseData.getGeneralAppParentCaseLink().getCaseReference())
        );
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        customProps.put(CASE_REFERENCE,
            Objects.requireNonNull(caseData.getGeneralAppParentCaseLink().getCaseReference()));
        customProps.put(GA_APPLICATION_TYPE,
            Objects.requireNonNull(requiredGAType(caseData)));
        return customProps;
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
                        JUDICIAL_FORMATTER), DATE) : null
        );
        if (areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getWrittenRepConcurrentRepresentationRespondentEmailTemplate()
            );
        }

        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getWrittenRepConcurrentRepresentationApplicantEmailTemplate()
        );
        customProps.remove(GA_JUDICIAL_CONCURRENT_DATE_TEXT);
    }

    private void sequentialWrittenRepNotification(CaseData caseData) {

        var sequentialDateTextRespondent = Optional.ofNullable(caseData
                                                        .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                                        .getSequentialApplicantMustRespondWithin()).orElse(null);

        customProps.put(
            GA_JUDICIAL_SEQUENTIAL_DATE_TEXT_RESPONDENT,
                    Objects.nonNull(sequentialDateTextRespondent)
                        ? DateFormatHelper
                        .formatLocalDate(
                            LocalDate.parse(
                                sequentialDateTextRespondent.toString(),
                                JUDICIAL_FORMATTER), DATE) : null
        );

        if (areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getWrittenRepSequentialRepresentationRespondentEmailTemplate()
            );
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
                        JUDICIAL_FORMATTER), DATE) : null
        );

        if (areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getJudgeRequestForInformationRespondentEmailTemplate()
            );
        }

        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeRequestForInformationApplicantEmailTemplate()
        );

        customProps.remove(GA_REQUEST_FOR_INFORMATION_DEADLINE);
    }

    private void applicationApprovedNotification(CaseData caseData) {
        String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();
        if (areRespondentSolicitorsPresent(caseData)
            && useDamageTemplate(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getJudgeApproveOrderToStrikeOutDamages()
            );
        } else if (areRespondentSolicitorsPresent(caseData)
            && useOcmcTemplate(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getJudgeApproveOrderToStrikeOutOCMC()
            );
        } else if (areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getJudgeForApproveRespondentEmailTemplate()
            );
        }

        if (useApplicantTemplate(caseData) && useDamageTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeApproveOrderToStrikeOutDamages()
            );
        } else if (useApplicantTemplate(caseData) && useOcmcTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeApproveOrderToStrikeOutOCMC()
            );
        } else if (useApplicantTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeForApprovedCaseApplicantEmailTemplate()
            );
        }

        if (useUncloakTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeUncloakApplicationEmailTemplate()
            );
        }
    }

    private void applicationListForHearing(CaseData caseData) {
        if (areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getJudgeListsForHearingRespondentEmailTemplate()
            );
        }

        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeListsForHearingApplicantEmailTemplate()
        );
    }

    private void applicationDismissedByJudge(CaseData caseData) {
        String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();
        if (areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(
                caseData,
                notificationProperties.getJudgeDismissesOrderRespondentEmailTemplate()
            );
        }

        if (useApplicantTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeDismissesOrderApplicantEmailTemplate()
            );
        }
        if (useUncloakTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeUncloakApplicationEmailTemplate()
            );
        }
    }

    private void applicationDirectionOrder(CaseData caseData) {
        String appSolicitorEmail = caseData.getGeneralAppApplnSolicitor().getEmail();
        if (areRespondentSolicitorsPresent(caseData)) {
            sendEmailToRespondent(caseData, notificationProperties.getJudgeForDirectionOrderRespondentEmailTemplate());
        }
        if (useApplicantTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeForDirectionOrderApplicantEmailTemplate()
            );
        }
        if (useUncloakTemplate(caseData)) {
            sendNotificationForJudicialDecision(
                caseData,
                appSolicitorEmail,
                notificationProperties.getJudgeUncloakApplicationEmailTemplate()
            );
        }
    }

    private void judgeApprovedOrderApplicationCloak(CaseData caseData) {
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
                                                notificationProperties.getJudgeForApprovedCaseApplicantEmailTemplate());
        }
    }

    private void judgeDismissedOrderApplicationCloak(CaseData caseData) {
        sendNotificationForJudicialDecision(caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeDismissesOrderApplicantEmailTemplate());
    }

    private void applicationDirectionOrderCloak(CaseData caseData) {
        sendNotificationForJudicialDecision(
            caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getJudgeForDirectionOrderApplicantEmailTemplate());
    }

    private void sendEmailToRespondent(CaseData caseData, String notificationProperties) {
        caseData.getGeneralAppRespondentSolicitors().forEach(
            respondentSolicitor -> sendNotificationForJudicialDecision(caseData,
                                                                       respondentSolicitor.getValue().getEmail(),
                                                                       notificationProperties
            ));
    }

    public static boolean useApplicantTemplate(CaseData caseData) {
        return caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES)
            || caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(YES);
    }

    public static boolean useUncloakTemplate(CaseData caseData) {
        return caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(NO);
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
