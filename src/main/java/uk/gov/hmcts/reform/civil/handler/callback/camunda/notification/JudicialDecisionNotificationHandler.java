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
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_NOTIFICATION_PROCESS_MAKE_DECISION;
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
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        switch (notificationCriterion(caseData)) {
            case APPLICATION_MOVES_TO_WITH_NOTICE:
                sendNotificationForJudicialDecision(
                    caseData, caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getWithNoticeUpdateRespondentEmailTemplate());
                break;
            case CONCURRENT_WRITTEN_REP:
                concurrentWrittenRepNotification(caseData);
                break;
            case SEQUENTIAL_WRITTEN_REP:
                sequentialWrittenRepNotification(caseData);
                break;
            case APPLICANT_WRITTEN_REP_CONCURRENT:
                applicantWrittenRepConcurrentNotification(caseData);
                break;
            case APPLICANT_WRITTEN_REP_SEQUENTIAL:
                sendNotificationForJudicialDecision(caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getApplicantWrittenRepSequentialRepresentationEmailTemplate());
                break;
            case JUDGE_DISMISSED_APPLICATION:
                sendNotificationForJudicialDecision(caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getJudgeDismissesOrderApplicantEmailTemplate());
                break;
            case LIST_FOR_HEARING:
                sendNotificationForJudicialDecision(caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getJudgeListsForHearingApplicantEmailTemplate());
                break;
            case APPLICATION_UNCLOAK:
                sendNotificationForJudicialDecision(caseData,
                    caseData.getGeneralAppApplnSolicitor().getEmail(),
                    notificationProperties.getJudgeUncloaksApplicationApplicantEmailTemplate());
                break;
            default: case NON_CRITERION:
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
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
            Objects.requireNonNull(requiredGAType(caseData.getGeneralAppType().getTypes())));
        return customProps;
    }

    private void concurrentWrittenRepNotification(CaseData caseData) {
        var concurrentDateText = Optional.ofNullable(caseData.getJudicialConcurrentDateText()).orElse(null);
        customProps.put(
            GA_JUDICIAL_CONCURRENT_DATE_TEXT,
            Objects.nonNull(concurrentDateText)
                ? concurrentDateText : null);
        caseData.getGeneralAppRespondentSolicitors().forEach(
            respondentSolicitor -> sendNotificationForJudicialDecision(
            caseData, respondentSolicitor.getValue().getEmail(),
            notificationProperties.getRespondentWrittenRepConcurrentRepresentationEmailTemplate()));
        sendNotificationForJudicialDecision(caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getApplicantWrittenRepConcurrentRepresentationEmailTemplate());
        customProps.remove(GA_JUDICIAL_CONCURRENT_DATE_TEXT);
    }

    private void sequentialWrittenRepNotification(CaseData caseData) {
        var sequentialDateText = Optional.ofNullable(caseData.getJudicialSequentialDateText()).orElse(null);
        customProps.put(
            GA_JUDICIAL_SEQUENTIAL_DATE_TEXT,
            Objects.nonNull(sequentialDateText)
                ? sequentialDateText : null);
        caseData.getGeneralAppRespondentSolicitors().forEach(
            respondentSolicitor -> sendNotificationForJudicialDecision(
            caseData, respondentSolicitor.getValue().getEmail(),
            notificationProperties.getRespondentWrittenRepSequentialRepresentationEmailTemplate()));
        sendNotificationForJudicialDecision(caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getApplicantWrittenRepSequentialRepresentationEmailTemplate());
        customProps.remove(GA_JUDICIAL_SEQUENTIAL_DATE_TEXT);
    }

    private void applicantWrittenRepConcurrentNotification(CaseData caseData) {
        var concurrentDateText = Optional.ofNullable(caseData.getJudicialConcurrentDateText()).orElse(null);
        customProps.put(
            GA_JUDICIAL_CONCURRENT_DATE_TEXT,
            Objects.nonNull(concurrentDateText)
                ? concurrentDateText : null);
        sendNotificationForJudicialDecision(caseData,
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            notificationProperties.getApplicantWrittenRepConcurrentRepresentationEmailTemplate());
        customProps.remove(GA_JUDICIAL_CONCURRENT_DATE_TEXT);
    }
}
