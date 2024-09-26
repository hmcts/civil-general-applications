package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.PostOrderCoverLetter;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentMetaData;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadException;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.service.stitching.CivilDocumentStitchingService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.POST_ORDER_COVER_LETTER_LIP;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isWithNotice;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendFinalOrderPrintService implements TemplateDataGenerator<PostOrderCoverLetter> {

    private static final String LIP_APPLICANT = "Applicant";
    private static final String LIP_RESPONDENT = "Respondent";

    private final BulkPrintService bulkPrintService;
    private final DocumentDownloadService documentDownloadService;
    private final GaForLipService gaForLipService;

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;

    private final CivilDocumentStitchingService civilDocumentStitchingService;

    private static final String FINAL_ORDER_PACK_LETTER_TYPE = "final-order-document-pack";

    @Override
    public PostOrderCoverLetter getTemplateData(CaseData caseData, String userType) {

        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        boolean parentClaimantIsApplicant = identifyParentClaimantIsApplicant(caseData);

        return PostOrderCoverLetter.builder()
            .caseNumber(caseData.getCcdCaseReference().toString())
            .partyName(getPartyName(parentClaimantIsApplicant, userType, civilCaseData))
            .partyAddressAddressLine1(partyAddressAddressLine1(parentClaimantIsApplicant, userType, civilCaseData))
            .partyAddressAddressLine2(partyAddressAddressLine2(parentClaimantIsApplicant, userType, civilCaseData))
            .partyAddressAddressLine3(partyAddressAddressLine3(parentClaimantIsApplicant, userType, civilCaseData))
            .partyAddressPostCode(partyAddressPostCode(parentClaimantIsApplicant, userType, civilCaseData))
            .partyAddressPostTown(partyAddressPostTown(parentClaimantIsApplicant, userType, civilCaseData))
            .build();
    }

    private DocmosisDocument generate(CaseData caseData, String userType) {

        return documentGeneratorService.generateDocmosisDocument(
            getTemplateData(caseData, userType),
            POST_ORDER_COVER_LETTER_LIP
        );
    }

    public void sendJudgeFinalOrderToPrintForLIP(String authorisation, CaseDocument judgeOrderdocument, CaseData caseData, String userType) {

        List<String> recipients = new ArrayList<>();

        DocmosisDocument coverLetter = generate(caseData, userType);

        CaseDocument coverLetterCaseDocument = documentManagementService.uploadDocument(
            authorisation,
            new PDF(
                POST_ORDER_COVER_LETTER_LIP.getDocumentTitle(),
                coverLetter.getBytes(),
                DocumentType.POST_ORDER_COVER_LETTER_LIP
            )
        );

        List<DocumentMetaData> documentMetaDataList
            = stitchCoverLetterAndOrderDocument(coverLetterCaseDocument, judgeOrderdocument);

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

        if (gaForLipService.isLipApp(caseData) && Objects.nonNull(caseData.getClaimant1PartyName())) {
            recipients.add(caseData.getClaimant1PartyName());
        }

        if (gaForLipService.isLipResp(caseData)
            && isWithNotice(caseData)
            && Objects.nonNull(caseData.getDefendant1PartyName())) {
            recipients.add(caseData.getDefendant1PartyName());
        }

        sendBulkPrint(letterContent, caseData, recipients);

    }

    private void sendBulkPrint(byte[] letterContent, CaseData caseData, List<String> recipients) {

        bulkPrintService.printLetter(letterContent, caseData.getLegacyCaseReference(),
                                     caseData.getLegacyCaseReference(),
                                     SendFinalOrderPrintService.FINAL_ORDER_PACK_LETTER_TYPE, recipients);
    }

    private List<DocumentMetaData> stitchCoverLetterAndOrderDocument(CaseDocument coverLetterCaseDocument, CaseDocument judgeOrderdocument) {
        List<DocumentMetaData> documentMetaDataList = new ArrayList<>();

        documentMetaDataList.add(new DocumentMetaData(coverLetterCaseDocument.getDocumentLink(),
                                                      "Post order cover letter",
                                                      LocalDate.now().toString()));
        documentMetaDataList.add(new DocumentMetaData(
            judgeOrderdocument.getDocumentLink(),
            "Sealed Judge final order form",
            LocalDate.now().toString()
        ));

        return documentMetaDataList;
    }

    private boolean identifyParentClaimantIsApplicant(CaseData caseData) {

        return caseData.getParentClaimantIsApplicant() == null
            || YES.equals(caseData.getParentClaimantIsApplicant());

    }

    private String getPartyName(boolean parentClaimantIsApplicant, String userType, CaseData civilCaseData) {

        if (userType.equals(LIP_APPLICANT)) {
            return parentClaimantIsApplicant
                ? civilCaseData.getApplicant1().getPartyName()
                : civilCaseData.getRespondent1().getPartyName();
        } else {
            return parentClaimantIsApplicant
                ? civilCaseData.getRespondent1().getPartyName()
                : civilCaseData.getApplicant1().getPartyName();
        }
    }

    private String partyAddressAddressLine1(boolean parentClaimantIsApplicant, String userType, CaseData civilCaseData) {

        if (userType.equals(LIP_APPLICANT)) {
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

    private String partyAddressAddressLine2(boolean parentClaimantIsApplicant, String userType, CaseData civilCaseData) {
        if (userType.equals(LIP_APPLICANT)) {
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

    private String partyAddressAddressLine3(boolean parentClaimantIsApplicant, String userType, CaseData civilCaseData) {
        if (userType.equals(LIP_APPLICANT)) {
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

    private String partyAddressPostCode(boolean parentClaimantIsApplicant, String userType, CaseData civilCaseData) {
        if (userType.equals(LIP_APPLICANT)) {
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

    private String partyAddressPostTown(boolean parentClaimantIsApplicant, String userType, CaseData civilCaseData) {
        if (userType.equals(LIP_APPLICANT)) {
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
