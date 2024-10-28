package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadException;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadService;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowFlag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendFinalOrderPrintService {

    private final BulkPrintService bulkPrintService;
    private final DocumentDownloadService documentDownloadService;

    private static final String FINAL_ORDER_PACK_LETTER_TYPE = "final-order-document-pack";

    public void sendJudgeFinalOrderToPrintForLIP(String authorisation, Document postJudgeOrderDocument, CaseData caseData, CaseData civilCaseData, FlowFlag lipUserType) {

        List<String> recipients = new ArrayList<>();
        boolean parentClaimantIsApplicant = caseData.identifyParentClaimantIsApplicant(caseData);

        String documentUrl = postJudgeOrderDocument.getDocumentUrl();
        String documentId = documentUrl.substring(documentUrl.lastIndexOf("/") + 1);

        byte[] letterContent;

        try {
            letterContent = documentDownloadService.downloadDocument(authorisation, documentId).file().getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Failed getting letter content for Pip Stitched Letter ");
            throw new DocumentDownloadException(postJudgeOrderDocument.getDocumentFileName(), e);
        }

        if (lipUserType.equals(FlowFlag.POST_JUDGE_ORDER_LIP_APPLICANT) && Objects.nonNull(caseData.getClaimant1PartyName())) {
            recipients.add(caseData.getPartyName(parentClaimantIsApplicant, lipUserType, civilCaseData));

        }

        if (lipUserType.equals(FlowFlag.POST_JUDGE_ORDER_LIP_RESPONDENT)
            && Objects.nonNull(caseData.getDefendant1PartyName())) {
            recipients.add(caseData.getPartyName(parentClaimantIsApplicant, lipUserType, civilCaseData));
        }

        bulkPrintService.printLetter(letterContent, caseData.getCcdCaseReference().toString(),
                                     civilCaseData.getLegacyCaseReference(),
                                     SendFinalOrderPrintService.FINAL_ORDER_PACK_LETTER_TYPE, recipients);

    }
}