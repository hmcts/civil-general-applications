package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.PostOrderCoverLetter;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.documents.DocumentMetaData;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadException;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowFlag;
import uk.gov.hmcts.reform.civil.service.stitching.CivilDocumentStitchingService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.SEND_TRANSLATED_ORDER_TO_LIP_RESPONDENT;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.POST_ORDER_COVER_LETTER_LIP;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendFinalOrderPrintService {

    private final BulkPrintService bulkPrintService;
    private final DocumentDownloadService documentDownloadService;
    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;

    private final CivilDocumentStitchingService civilDocumentStitchingService;

    private static final String FINAL_ORDER_PACK_LETTER_TYPE = "final-order-document-pack";
    private static final String TRANSLATED_ORDER_PACK_LETTER_TYPE = "translated-order-document-pack";
    private static final String LIP_APPLICANT = "LIP_APPLICANT";

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

    public void sendJudgeTranslatedOrderToPrintForLIP(String authorisation, Document orderDocument, CaseData caseData, CaseEvent caseEvent) {

        List<String> recipients = new ArrayList<>();
        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        DocmosisDocument coverLetter = generate(caseData, civilCaseData, caseEvent);

        CaseDocument coverLetterCaseDocument = documentManagementService.uploadDocument(
            authorisation,
            new PDF(
                POST_ORDER_COVER_LETTER_LIP.getDocumentTitle(),
                coverLetter.getBytes(),
                DocumentType.POST_ORDER_COVER_LETTER_LIP
            )
        );

        List<DocumentMetaData> documentMetaDataList
            = stitchCoverLetterAndOrderDocument(coverLetterCaseDocument, orderDocument);

        CaseDocument stitchedDocument = civilDocumentStitchingService.bundle(
            documentMetaDataList,
            authorisation,
            coverLetterCaseDocument.getDocumentName(),
            coverLetterCaseDocument.getDocumentName(),
            caseData
        );

        String documentUrl = stitchedDocument.getDocumentLink().getDocumentUrl();
        String documentId = documentUrl.substring(documentUrl.lastIndexOf("/") + 1);

        byte[] letterContent;

        try {
            letterContent = documentDownloadService.downloadDocument(authorisation, documentId).file().getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Failed getting letter content for Pip Stitched Letter ");
            throw new DocumentDownloadException(stitchedDocument.getDocumentLink().getDocumentFileName(), e);
        }

        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            String applicant = caseData.getParentClaimantIsApplicant() == YES
                ? civilCaseData.getApplicant1().getPartyName()
                : civilCaseData.getRespondent1().getPartyName();
            recipients.add(applicant);
        }

        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_RESPONDENT) {
            String respondent = caseData.getParentClaimantIsApplicant() == YES
                ? civilCaseData.getRespondent1().getPartyName()
                : civilCaseData.getApplicant1().getPartyName();
            recipients.add(respondent);
        }

        sendBulkPrint(letterContent, caseData, civilCaseData, recipients);
    }

    private void sendBulkPrint(byte[] letterContent, CaseData caseData, CaseData civilCaseData, List<String> recipients) {

        bulkPrintService.printLetter(letterContent, caseData.getCcdCaseReference().toString(),
                                     civilCaseData.getLegacyCaseReference(),
                                     SendFinalOrderPrintService.TRANSLATED_ORDER_PACK_LETTER_TYPE, recipients);
    }

    private List<DocumentMetaData> stitchCoverLetterAndOrderDocument(CaseDocument coverLetterCaseDocument, Document orderDocument) {
        List<DocumentMetaData> documentMetaDataList = new ArrayList<>();

        documentMetaDataList.add(new DocumentMetaData(coverLetterCaseDocument.getDocumentLink(),
                                                      "Post order cover letter",
                                                      LocalDate.now().toString()));
        documentMetaDataList.add(new DocumentMetaData(
            orderDocument,
            "Translated judge order",
            LocalDate.now().toString()
        ));

        return documentMetaDataList;
    }

    private boolean identifyParentClaimantIsApplicant(CaseData caseData) {
        return caseData.getParentClaimantIsApplicant() == null
            || YES.equals(caseData.getParentClaimantIsApplicant());
    }

    private DocmosisDocument generate(CaseData caseData, CaseData civilCaseData, CaseEvent caseEvent) {

        return documentGeneratorService.generateDocmosisDocument(
            getTemplateData(caseData, civilCaseData, caseEvent),
            POST_ORDER_COVER_LETTER_LIP
        );
    }

    public PostOrderCoverLetter getTemplateData(CaseData caseData, CaseData civilCaseData, CaseEvent caseEvent) {
        boolean parentClaimantIsApplicant = identifyParentClaimantIsApplicant(caseData);

        return PostOrderCoverLetter.builder()
            .caseNumber(caseData.getCcdCaseReference().toString())
            .partyName(getPartyName(parentClaimantIsApplicant, caseEvent, civilCaseData))
            .partyAddressAddressLine1(partyAddressAddressLine1(parentClaimantIsApplicant, caseEvent, civilCaseData))
            .partyAddressAddressLine2(partyAddressAddressLine2(parentClaimantIsApplicant, caseEvent, civilCaseData))
            .partyAddressAddressLine3(partyAddressAddressLine3(parentClaimantIsApplicant, caseEvent, civilCaseData))
            .partyAddressPostCode(partyAddressPostCode(parentClaimantIsApplicant, caseEvent, civilCaseData))
            .partyAddressPostTown(partyAddressPostTown(parentClaimantIsApplicant, caseEvent, civilCaseData))
            .build();
    }

    private String getPartyName(boolean parentClaimantIsApplicant, CaseEvent caseEvent, CaseData civilCaseData) {

        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            return parentClaimantIsApplicant
                ? civilCaseData.getApplicant1().getPartyName()
                : civilCaseData.getRespondent1().getPartyName();
        } else {
            return parentClaimantIsApplicant
                ? civilCaseData.getRespondent1().getPartyName()
                : civilCaseData.getApplicant1().getPartyName();
        }
    }

    private String partyAddressAddressLine1(boolean parentClaimantIsApplicant, CaseEvent caseEvent, CaseData civilCaseData) {

        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getAddressLine1())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getAddressLine1())
                .orElse(StringUtils.EMPTY);
        } else {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getAddressLine1())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getAddressLine1())
                .orElse(StringUtils.EMPTY);
        }
    }

    private String partyAddressAddressLine2(boolean parentClaimantIsApplicant, CaseEvent caseEvent, CaseData civilCaseData) {
        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getAddressLine2())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getAddressLine2())
                .orElse(StringUtils.EMPTY);
        } else {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getAddressLine2())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getAddressLine2())
                .orElse(StringUtils.EMPTY);
        }
    }

    private String partyAddressAddressLine3(boolean parentClaimantIsApplicant, CaseEvent caseEvent, CaseData civilCaseData) {
        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getAddressLine3())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getAddressLine3())
                .orElse(StringUtils.EMPTY);
        } else {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getAddressLine3())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getAddressLine3())
                .orElse(StringUtils.EMPTY);
        }
    }

    private String partyAddressPostCode(boolean parentClaimantIsApplicant, CaseEvent caseEvent, CaseData civilCaseData) {
        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getPostCode())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getPostCode())
                .orElse(StringUtils.EMPTY);
        } else {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getPostCode())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getPostCode())
                .orElse(StringUtils.EMPTY);
        }
    }

    private String partyAddressPostTown(boolean parentClaimantIsApplicant, CaseEvent caseEvent, CaseData civilCaseData) {
        if (caseEvent == SEND_TRANSLATED_ORDER_TO_LIP_APPLICANT) {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getPostTown())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getPostTown())
                .orElse(StringUtils.EMPTY);
        } else {
            return parentClaimantIsApplicant
                ? ofNullable(civilCaseData.getRespondent1().getPrimaryAddress().getPostTown())
                .orElse(StringUtils.EMPTY)
                : ofNullable(civilCaseData.getApplicant1().getPrimaryAddress().getPostTown())
                .orElse(StringUtils.EMPTY);
        }
    }
}
