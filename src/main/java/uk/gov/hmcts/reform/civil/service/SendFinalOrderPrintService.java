package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadException;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isWithNotice;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendFinalOrderPrintService {

    private final BulkPrintService bulkPrintService;
    private final DocumentDownloadService documentDownloadService;
    private final GaForLipService gaForLipService;

    private static final String FINAL_ORDER_PACK_LETTER_TYPE = "final-order-document-pack";

    public void sendJudgeFinalOrderToPrintForLIP(String authorisation, Document document, CaseData caseData) {

        List<String> recipients = new ArrayList<>();

        if (gaForLipService.isLipApp(caseData) && Objects.nonNull(caseData.getClaimant1PartyName())) {
            recipients.add(caseData.getClaimant1PartyName());
        }

        if (gaForLipService.isLipResp(caseData)
            && isWithNotice(caseData)
            && Objects.nonNull(caseData.getDefendant1PartyName())) {
            recipients.add(caseData.getDefendant1PartyName());
        }

        sendBulkPrint(authorisation, caseData, document, recipients);

    }

    private void sendBulkPrint(String authorisation, CaseData caseData, Document document, List<String> recipients) {
        String documentUrl = document.getDocumentUrl();
        String documentId = documentUrl.substring(documentUrl.lastIndexOf("/") + 1);
        byte[] letterContent;
        try {
            letterContent = documentDownloadService.downloadDocument(authorisation, documentId).file().getInputStream().readAllBytes();
        } catch (Exception e) {
            log.error("Failed getting letter content for Final Order ");
            throw new DocumentDownloadException(document.getDocumentFileName(), e);
        }
        bulkPrintService.printLetter(letterContent, caseData.getLegacyCaseReference(),
                                     caseData.getLegacyCaseReference(),
                                     SendFinalOrderPrintService.FINAL_ORDER_PACK_LETTER_TYPE, recipients);
    }
}
