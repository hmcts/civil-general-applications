package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseevents;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocument;
import uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.service.SendFinalOrderPrintService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.SEND_TRANSLATED_ORDER_TO_LIP_RESPONDENT;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.APPROVE_OR_EDIT_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.DISMISSAL_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.HEARING_NOTICE;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.HEARING_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.JUDGES_DIRECTIONS_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.REQUEST_FOR_MORE_INFORMATION_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.WRITTEN_REPRESENTATIONS_ORDER_CONCURRENT;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.WRITTEN_REPRESENTATIONS_ORDER_SEQUENTIAL;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isWithNotice;

@Service
@RequiredArgsConstructor
public class SendTranslatedOrderToLiPCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT, SEND_TRANSLATED_ORDER_TO_LIP_RESPONDENT);

    private final SendFinalOrderPrintService sendFinalOrderPrintService;
    private static final List<TranslatedDocumentType> POST_TRANSLATED_DOCUMENT_TYPES = List.of(
        REQUEST_FOR_MORE_INFORMATION_ORDER,
        HEARING_ORDER,
        GENERAL_ORDER,
        DISMISSAL_ORDER,
        JUDGES_DIRECTIONS_ORDER,
        WRITTEN_REPRESENTATIONS_ORDER_SEQUENTIAL,
        WRITTEN_REPRESENTATIONS_ORDER_CONCURRENT,
        HEARING_NOTICE,
        APPROVE_OR_EDIT_ORDER
    );

    @Value("${print.service.enabled}")
    public boolean printServiceEnabled;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::sendTranslatedOrderLetter);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse sendTranslatedOrderLetter(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseEvent caseEvent = CaseEvent.valueOf(callbackParams.getRequest().getEventId());
        if (printServiceEnabled && shouldPrintTranslatedDocument(caseData, caseEvent)) {
            TranslatedDocument translatedDocument = caseData.getTranslatedDocumentsBulkPrint().get(caseData.getTranslatedDocumentsBulkPrint().size() - 1)
                .getValue();
            sendFinalOrderPrintService.sendJudgeTranslatedOrderToPrintForLIP(
                callbackParams.getParams().get(BEARER_TOKEN).toString(),
                translatedDocument.getFile(),
                caseData,
                caseEvent);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .build();
    }

    private boolean shouldPrintTranslatedDocument(CaseData caseData, CaseEvent caseEvent) {
        boolean isUserBilingual = caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT
            ? caseData.getApplicantBilingualLanguagePreference() == YesOrNo.YES
            : caseData.getRespondentBilingualLanguagePreference() == YesOrNo.YES;
        if (isUserBilingual && isWithNoticeIfRespondent(caseData, caseEvent) && isDocumentCorrectType(caseData)) {
            return true;
        }
        return false;
    }

    private boolean isDocumentCorrectType(CaseData caseData) {
        List<Element<TranslatedDocument>> translatedDocuments = caseData.getTranslatedDocumentsBulkPrint();
        if (translatedDocuments == null || translatedDocuments.size() == 0) {
            return false;
        }
        TranslatedDocumentType documentType = translatedDocuments.get(translatedDocuments.size() - 1).getValue().getDocumentType();
        return POST_TRANSLATED_DOCUMENT_TYPES.contains(documentType);
    }

    private boolean isWithNoticeIfRespondent(CaseData caseData, CaseEvent caseEvent) {
        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            return true;
        }
        return isWithNotice(caseData);
    }
}
