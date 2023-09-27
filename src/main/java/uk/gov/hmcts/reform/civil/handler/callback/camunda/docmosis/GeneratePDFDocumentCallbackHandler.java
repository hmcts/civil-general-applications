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
import uk.gov.hmcts.reform.civil.service.docmosis.consentorder.ConsentOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.directionorder.DirectionOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder.DismissalOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.AssistedOrderFormGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.FreeFormOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.generalorder.GeneralOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder.WrittenRepresentationConcurrentOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder.WrittenRepresentationSequentailOrderGenerator;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_JUDGES_FORM;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection.ASSISTED_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection.FREE_FORM_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;
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
    private final AssistedOrderFormGenerator assistedOrderFormGenerator;
    private final ConsentOrderGenerator consentOrderGenerator;
    private final ObjectMapper objectMapper;

    private final AssignCategoryId assignCategoryId;

    @Override
    public String camundaActivityId() {
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

        /*
         * Setting up the CategoryID for Order documents will be covered under CIV-8316
         * as it has dependency on CIV-7926.
         *
         * Uncomment the assignCategoryID code for setting up the categoryID after CIV-7926 is merged in Civil repo
         * */

        CaseData caseData = callbackParams.getCaseData();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        CaseDocument decision = null;
        if (Objects.nonNull(caseData.getApproveConsentOrder())) {
            decision = consentOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> consentOrderDocumentList =
                ofNullable(caseData.getConsentOrderDocument()).orElse(newArrayList());

            consentOrderDocumentList.addAll(wrapElements(decision));

            assignCategoryId.assignCategoryIdToCollection(consentOrderDocumentList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.ORDER_DOCUMENTS);
            caseDataBuilder.consentOrderDocument(consentOrderDocumentList);
        } else if (Objects.nonNull(caseData.getFinalOrderSelection())) {
            if (caseData.getFinalOrderSelection().equals(FREE_FORM_ORDER)) {
                decision = freeFormOrderGenerator.generate(
                        caseDataBuilder.build(),
                        callbackParams.getParams().get(BEARER_TOKEN).toString()
                );
            } else if (caseData.getFinalOrderSelection().equals(ASSISTED_ORDER)) {
                decision = assistedOrderFormGenerator.generate(
                        caseDataBuilder.build(),
                        callbackParams.getParams().get(BEARER_TOKEN).toString()
                );
            }
            List<Element<CaseDocument>> newGeneralOrderDocumentList =
                    ofNullable(caseData.getGeneralOrderDocument()).orElse(newArrayList());

            newGeneralOrderDocumentList.addAll(wrapElements(decision));

            assignCategoryId.assignCategoryIdToCollection(newGeneralOrderDocumentList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.ORDER_DOCUMENTS);
            caseDataBuilder.generalOrderDocument(newGeneralOrderDocumentList);
        } else if (isGeneralOrder(caseData)) {
            decision = generalOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            assignCategoryId.assignCategoryIdToCaseDocument(decision,
                                                            AssignCategoryId.ORDER_DOCUMENTS);

            caseDataBuilder.generalOrderDocument(wrapElements(decision));
        } else if (isDirectionOrder(caseData)) {
            decision = directionOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> newDirectionOrderDocumentList =
                ofNullable(caseData.getDirectionOrderDocument()).orElse(newArrayList());

            newDirectionOrderDocumentList.addAll(wrapElements(decision));

            assignCategoryId.assignCategoryIdToCollection(newDirectionOrderDocumentList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.ORDER_DOCUMENTS);
            caseDataBuilder.directionOrderDocument(newDirectionOrderDocumentList);

        } else if (isDismissalOrder(caseData)) {
            decision = dismissalOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            assignCategoryId.assignCategoryIdToCaseDocument(decision,
                                                            AssignCategoryId.ORDER_DOCUMENTS);

            caseDataBuilder.dismissalOrderDocument(wrapElements(decision));
        } else if (isHearingOrder(caseData)) {
            decision = hearingOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            assignCategoryId.assignCategoryIdToCaseDocument(decision,
                                                            AssignCategoryId.APPLICATIONS);

            caseDataBuilder.hearingOrderDocument(wrapElements(decision));
        } else if (isWrittenRepSeqOrder(caseData)) {
            decision = writtenRepresentationSequentailOrderGenerator.generate(
                    caseDataBuilder.build(),
                    callbackParams.getParams().get(BEARER_TOKEN).toString()
                );

            List<Element<CaseDocument>> newWrittenRepSequentialDocumentList =
                ofNullable(caseData.getWrittenRepSequentialDocument()).orElse(newArrayList());

            newWrittenRepSequentialDocumentList.addAll(wrapElements(decision));

            assignCategoryId.assignCategoryIdToCollection(newWrittenRepSequentialDocumentList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.APPLICATIONS);
            caseDataBuilder.writtenRepSequentialDocument(newWrittenRepSequentialDocumentList);

        } else if (isWrittenRepConOrder(caseData)) {
            decision = writtenRepresentationConcurrentOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> newWrittenRepConcurrentDocumentList =
                ofNullable(caseData.getWrittenRepConcurrentDocument()).orElse(newArrayList());

            newWrittenRepConcurrentDocumentList.addAll(wrapElements(decision));
            assignCategoryId.assignCategoryIdToCollection(newWrittenRepConcurrentDocumentList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.APPLICATIONS);

            caseDataBuilder.writtenRepConcurrentDocument(newWrittenRepConcurrentDocumentList);

        } else if (isRequestMoreInfo(caseData)) {
            decision = requestForInformationGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> newRequestForInfoDocumentList =
                ofNullable(caseData.getRequestForInformationDocument()).orElse(newArrayList());

            newRequestForInfoDocumentList.addAll(wrapElements(decision));

            assignCategoryId.assignCategoryIdToCollection(newRequestForInfoDocumentList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.APPLICATIONS
            );

            caseDataBuilder.requestForInformationDocument(newRequestForInfoDocumentList);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private boolean isRequestMoreInfo(final CaseData caseData) {
        return caseData.getJudicialDecision().getDecision().equals(REQUEST_MORE_INFO)
                && caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoByDate() != null
                && caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoText() != null;
    }

    private boolean isWrittenRepConOrder(final CaseData caseData) {
        return caseData.getJudicialDecision().getDecision().equals(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS)
                && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenConcurrentRepresentationsBy() != null
                && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenOption().equals(CONCURRENT_REPRESENTATIONS)
                ;
    }

    private boolean isWrittenRepSeqOrder(final CaseData caseData) {
        return caseData.getJudicialDecision().getDecision().equals(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS)
                && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenSequentailRepresentationsBy() != null
                && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getSequentialApplicantMustRespondWithin() != null&& caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenOption().equals(SEQUENTIAL_REPRESENTATIONS);
    }

    private boolean isHearingOrder(final CaseData caseData) {
        return caseData.getJudicialDecision().getDecision().equals(LIST_FOR_A_HEARING)
                && caseData.getJudicialListForHearing() != null;
    }

    private boolean isDismissalOrder(final CaseData caseData) {
        return caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
                && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(DISMISS_THE_APPLICATION);
    }

    private boolean isDirectionOrder(final CaseData caseData) {
        return caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
                && caseData.getJudicialDecisionMakeOrder().getDirectionsText() != null
                && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(GIVE_DIRECTIONS_WITHOUT_HEARING);
    }

    private boolean isGeneralOrder(CaseData caseData) {
        return caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
                && caseData.getJudicialDecisionMakeOrder().getOrderText() != null
                && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(APPROVE_OR_EDIT);
    }
}
