package uk.gov.hmcts.reform.civil.handler.callback.camunda.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.service.docmosis.directionorder.DirectionOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder.DismissalOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.FreeFormOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.generalorder.GeneralOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder.WrittenRepresentationConcurrentOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder.WrittenRepresentationSequentailOrderGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_JUDGES_FORM;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection.FREE_FORM_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@Service
@RequiredArgsConstructor
public class GeneratePDFDocumentCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(GENERATE_JUDGES_FORM);
    private static final String TASK_ID = "CreatePDFDocument";

    private final GeneralOrderGenerator generalOrderGenerator;
    private final RequestForInformationGenerator requestForInformationGenerator;
    private final DirectionOrderGenerator directionOrderGenerator;
    private final DismissalOrderGenerator dismissalOrderGenerator;
    private final HearingOrderGenerator hearingOrderGenerator;
    private final WrittenRepresentationSequentailOrderGenerator writtenRepresentationSequentailOrderGenerator;
    private final WrittenRepresentationConcurrentOrderGenerator writtenRepresentationConcurrentOrderGenerator;
    private final FreeFormOrderGenerator freeFormOrderGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::createPDFdocument);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse createPDFdocument(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        CaseDocument judgeDecision = null;
        if (Objects.nonNull(caseData.getFinalOrderSelection())) {
            if (caseData.getFinalOrderSelection().equals(FREE_FORM_ORDER)) {
                judgeDecision = freeFormOrderGenerator.generate(
                        caseDataBuilder.build(),
                        callbackParams.getParams().get(BEARER_TOKEN).toString()
                );
                List<Element<CaseDocument>> newGeneralOrderDocumentList =
                        ofNullable(caseData.getGeneralOrderDocument()).orElse(newArrayList());

                newGeneralOrderDocumentList.addAll(wrapElements(judgeDecision));
                caseDataBuilder.generalOrderDocument(newGeneralOrderDocumentList);
            }
        } else if (caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
            && caseData.getJudicialDecisionMakeOrder().getOrderText() != null
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(APPROVE_OR_EDIT)) {
            judgeDecision = generalOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );
            caseDataBuilder.generalOrderDocument(wrapElements(judgeDecision));
        } else if (caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
            && caseData.getJudicialDecisionMakeOrder().getDirectionsText() != null
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(GIVE_DIRECTIONS_WITHOUT_HEARING)) {
            judgeDecision = directionOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> newDirectionOrderDocumentList =
                ofNullable(caseData.getDirectionOrderDocument()).orElse(newArrayList());

            newDirectionOrderDocumentList.addAll(wrapElements(judgeDecision));

            caseDataBuilder.directionOrderDocument(newDirectionOrderDocumentList);

        } else if (caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(DISMISS_THE_APPLICATION)) {
            judgeDecision = dismissalOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );
            caseDataBuilder.dismissalOrderDocument(wrapElements(judgeDecision));
        } else if (caseData.getJudicialDecision().getDecision().equals(LIST_FOR_A_HEARING)
            && caseData.getJudicialListForHearing() != null) {
            judgeDecision = hearingOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );
            caseDataBuilder.hearingOrderDocument(wrapElements(judgeDecision));
        } else if (caseData.getJudicialDecision().getDecision().equals(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS)
                && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
            .getWrittenSequentailRepresentationsBy() != null
                && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
            .getSequentialApplicantMustRespondWithin() != null) {
            judgeDecision = writtenRepresentationSequentailOrderGenerator.generate(
                    caseDataBuilder.build(),
                    callbackParams.getParams().get(BEARER_TOKEN).toString()
                );

            List<Element<CaseDocument>> newWrittenRepSequentialDocumentList =
                ofNullable(caseData.getWrittenRepSequentialDocument()).orElse(newArrayList());

            newWrittenRepSequentialDocumentList.addAll(wrapElements(judgeDecision));

            caseDataBuilder.writtenRepSequentialDocument(newWrittenRepSequentialDocumentList);

        } else if (caseData.getJudicialDecision().getDecision().equals(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS)
            && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
            .getWrittenConcurrentRepresentationsBy() != null) {
            judgeDecision = writtenRepresentationConcurrentOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> newWrittenRepConcurrentDocumentList =
                ofNullable(caseData.getWrittenRepConcurrentDocument()).orElse(newArrayList());

            newWrittenRepConcurrentDocumentList.addAll(wrapElements(judgeDecision));

            caseDataBuilder.writtenRepConcurrentDocument(newWrittenRepConcurrentDocumentList);

        } else if (caseData.getJudicialDecision().getDecision().equals(REQUEST_MORE_INFO)
            && caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoByDate() != null
            && caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoText() != null) {
            judgeDecision = requestForInformationGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> newRequestForInfoDocumentList =
                ofNullable(caseData.getRequestForInformationDocument()).orElse(newArrayList());

            newRequestForInfoDocumentList.addAll(wrapElements(judgeDecision));

            caseDataBuilder.requestForInformationDocument(newRequestForInfoDocumentList);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }
}
