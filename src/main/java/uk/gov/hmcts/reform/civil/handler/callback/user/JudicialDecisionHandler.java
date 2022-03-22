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
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.JUDGE_MAKES_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@Service
@RequiredArgsConstructor
public class JudicialDecisionHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(JUDGE_MAKES_DECISION);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yy");
    private static final String VALIDATE_WRITTEN_REPRESENTATION_DATE = "ga-validate-written-representation-date";
    private static final String JUDICIAL_RECITAL_TEXT = "Upon reading the application of %s dated %s and upon the "
            + "application of %s dated %s and upon considering the information provided by the parties";
    private static final String DISMISSAL_ORDER_TEXT = "This application is dismissed.\n\n"
            + "[Insert Draft Order from application]\n\n"
            + "A person who was not notified of the application before this order was made may apply to have the "
            + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
            + "notification of the order.";
    private static final String DIRECTIONS_IN_RELATION_TO_HEARING_TEXT = "A person who was not notified of the application"
    +"before this order was made may apply to have this order set aside or varied. Any application under this paragraph"
    +"must be made within 7 days after notification of the order.";

    private final JudicialDecisionService judicialDecisionService;

    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::checkInputForNextPage,
            callbackKey(MID, VALIDATE_WRITTEN_REPRESENTATION_DATE), this::gaValidateWrittenRepresentationsDate);
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

        caseDataBuilder.judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData)).directionInRelationToHearingText(DIRECTIONS_IN_RELATION_TO_HEARING_TEXT).build();

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

    private CallbackResponse gaValidateWrittenRepresentationsDate(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        GAJudicialWrittenRepresentations judicialWrittenRepresentationsDate = caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations();
        List<String> errors = judicialWrittenRepresentationsDate != null
            ? judicialDecisionService.validateWrittenRepresentationsDates(judicialWrittenRepresentationsDate)
            : Collections.emptyList();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
