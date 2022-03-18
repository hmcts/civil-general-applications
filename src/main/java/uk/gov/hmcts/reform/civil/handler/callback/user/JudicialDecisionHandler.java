package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.JUDGE_MAKES_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;

@SuppressWarnings({"checkstyle:Indentation", "checkstyle:EmptyLineSeparator"})
@Service
@RequiredArgsConstructor
public class JudicialDecisionHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(JUDGE_MAKES_DECISION);
    private static final String VALIDATE_REQUEST_MORE_INFO_SCREEN = "validate-request-more-info-screen";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yy");
    private static final String JUDICIAL_RECITAL_TEXT = "Upon reading the application of %s dated %s and upon the "
            + "application of %s dated %s and upon considering the information provided by the parties";
    private static final String DISMISSAL_ORDER_TEXT = "This application is dismissed.\n\n"
            + "[Insert Draft Order from application]\n\n"
            + "A person who was not notified of the application before this order was made may apply to have the "
            + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
            + "notification of the order.";
    public static final String REQUESTED_MORE_INFO_BY_DATE_REQUIRED = "The date, by which the applicant must respond, "
            + "is required.";
    public static final String REQUESTED_MORE_INFO_BY_DATE_IN_PAST = "The date, by which the applicant must respond, "
            + "cannot be in past.";
    public static final String OTHER_PARTY_MORE_INFO_BY_DATE_REQUIRED = "The date, by which the other party must "
            + "respond, is required.";
    public static final String OTHER_PARTY_MORE_INFO_BY_DATE_IN_PAST = "The date, by which the other party must "
            + "respond, cannot be in past.";

    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_START), this::checkInputForNextPage,
                callbackKey(MID, VALIDATE_REQUEST_MORE_INFO_SCREEN), this::gaValidateRequestMoreInfoScreen);
    }

    private CallbackResponse checkInputForNextPage(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        YesOrNo isCloaked = (caseData.getGeneralAppRespondentAgreement() != null
                && NO.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
                && caseData.getGeneralAppInformOtherParty() != null
                && NO.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice()))
                ? YES : NO;
        caseDataBuilder.applicationIsCloaked(isCloaked);

        GAJudicialMakeAnOrder.GAJudicialMakeAnOrderBuilder makeAnOrderBuilder;
        if (caseData.getJudicialDecisionMakeOrder() != null) {
            makeAnOrderBuilder = caseData.getJudicialDecisionMakeOrder().toBuilder();
        } else {
            makeAnOrderBuilder = GAJudicialMakeAnOrder.builder();
        }
        caseDataBuilder.judicialDecisionMakeOrder(makeAnOrderBuilder
                .judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData))
                .dismissalOrderText(DISMISSAL_ORDER_TEXT).build());

        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private String getJudgeRecitalPrepopulatedText(CaseData caseData) {
        return format(JUDICIAL_RECITAL_TEXT,
                (caseData.getParentClaimantIsApplicant() == null
                        || YES.equals(caseData.getParentClaimantIsApplicant()))
                        ? "Claimant" : "Defendant",
                DATE_FORMATTER.format(caseData.getCreatedDate()),
                caseData.getApplicantPartyName(),
                DATE_FORMATTER.format(LocalDate.now()));
    }

    private CallbackResponse gaValidateRequestMoreInfoScreen(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        GAJudicialRequestMoreInfo judicialRequestMoreInfo = caseData.getJudicialDecisionRequestMoreInfo();
        List<String> errors = judicialRequestMoreInfo != null
                ? validateDatesForRequestMoreInfoScreen(judicialRequestMoreInfo)
                : Collections.emptyList();

        return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(errors)
                .build();
    }

    public List<String> validateDatesForRequestMoreInfoScreen(GAJudicialRequestMoreInfo judicialRequestMoreInfo) {
        List<String> errors = new ArrayList<>();
        if (REQUEST_MORE_INFORMATION.equals(judicialRequestMoreInfo.getRequestMoreInfoOption())) {
            if (judicialRequestMoreInfo.getJudgeRequestMoreInfoByDate() == null) {
                errors.add(REQUESTED_MORE_INFO_BY_DATE_REQUIRED);
            } else {
                if (LocalDate.now().isAfter(judicialRequestMoreInfo.getJudgeRequestMoreInfoByDate())) {
                    errors.add(REQUESTED_MORE_INFO_BY_DATE_IN_PAST);
                }
            }
        }
        if (SEND_APP_TO_OTHER_PARTY.equals(judicialRequestMoreInfo.getRequestMoreInfoOption())) {
            if (judicialRequestMoreInfo.getJudgeSendAppToOtherPartyResponseByDate() == null) {
                errors.add(OTHER_PARTY_MORE_INFO_BY_DATE_REQUIRED);
            } else {
                if (LocalDate.now().isAfter(judicialRequestMoreInfo.getJudgeSendAppToOtherPartyResponseByDate())) {
                    errors.add(OTHER_PARTY_MORE_INFO_BY_DATE_IN_PAST);
                }
            }
        }
        return errors;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}