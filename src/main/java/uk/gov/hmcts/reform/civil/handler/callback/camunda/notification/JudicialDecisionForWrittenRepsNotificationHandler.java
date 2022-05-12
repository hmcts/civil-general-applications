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
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_GA_APPLICANT_AND_RESPONDENT_FOR_WRITTEN_REPS;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isApplicationForConcurrentWrittenRep;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isApplicationForSequentialWrittenRep;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isNotificationCriteriaSatisfiedForWrittenReps;

@Service
@RequiredArgsConstructor
public class JudicialDecisionForWrittenRepsNotificationHandler extends CallbackHandler implements NotificationData {

    private final ObjectMapper objectMapper;
    private final NotificationsProperties notificationProperties;
    private final NotificationService notificationService;

    private final static String TO_STRIKE_OUT = "to strike out";
    private final static String FOR_SUMMARY_JUDGEMENT = "for summary judgment";
    private final static String TO_STAY_THE_CLAIM = "to stay the claim";
    private final static String TO_EXTEND_TIME = "to extend time";

    private static final List<CaseEvent> EVENTS = List.of(
        NOTIFY_GA_APPLICANT_AND_RESPONDENT_FOR_WRITTEN_REPS
    );

    private static final String REFERENCE_TEMPLATE = "general-application-respondent-notification-%s";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::notifyApplicantAndRespondentForWrittenReps
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse notifyApplicantAndRespondentForWrittenReps(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        if (isNotificationCriteriaSatisfiedForWrittenReps(caseData)) {
            List<Element<GASolicitorDetailsGAspec>> respondentSolicitors = caseData.getGeneralAppRespondentSolicitors();
            String writtenRepDynamicTemplate = null;

            if (isApplicationForConcurrentWrittenRep(caseData)) {
                writtenRepDynamicTemplate = notificationProperties.getWrittenRepConcurrentRepresentationTemplate();
            } else if (isApplicationForSequentialWrittenRep(caseData)) {
                writtenRepDynamicTemplate = notificationProperties.getWrittenRepSequentialRepresentationTemplate();
            }
            String finalWrittenRepDynamicTemplate = writtenRepDynamicTemplate;
            respondentSolicitors.forEach((
                                             respondentSolicitor) -> sendNotificationToGeneralAppRespondent(
                caseData, respondentSolicitor.getValue().getEmail(), finalWrittenRepDynamicTemplate));

            sendNotificationToGeneralAppRespondent(caseData, caseData.getGeneralAppApplnSolicitor().getEmail(),
                                                   finalWrittenRepDynamicTemplate);

        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private void sendNotificationToGeneralAppRespondent(CaseData caseData, String recipient, String template) {
        notificationService.sendMail(
            recipient,
            template,
            addProperties(caseData),
            String.format(REFERENCE_TEMPLATE, caseData.getGeneralAppParentCaseLink().getCaseReference())
        );
    }

    private String getRequiredGAType(List<GeneralApplicationTypes> gaType) {

        for (GeneralApplicationTypes type : gaType) {
            if (type.equals(GeneralApplicationTypes.STRIKE_OUT)) {
                return TO_STRIKE_OUT;
            } else if (type.equals(GeneralApplicationTypes.SUMMARY_JUDGEMENT)) {
                return FOR_SUMMARY_JUDGEMENT;
            } else if (type.equals(GeneralApplicationTypes.STAY_THE_CLAIM)) {
                return TO_STAY_THE_CLAIM;
            } else if (type.equals((GeneralApplicationTypes.EXTEND_TIME))) {
                return TO_EXTEND_TIME;
            }
        }
        return null;
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
            CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference(),
            GA_APPLICATION_TYPE, Objects.requireNonNull(getRequiredGAType(caseData.getGeneralAppType().getTypes()))
        );
    }
}
