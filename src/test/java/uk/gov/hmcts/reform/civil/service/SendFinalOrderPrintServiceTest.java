package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.Address;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Party;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.documents.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadService;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowFlag;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SendFinalOrderPrintServiceTest {

    @Mock
    private DocumentDownloadService documentDownloadService;

    @Mock
    private BulkPrintService bulkPrintService;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private SendFinalOrderPrintService sendFinalOrderPrintService;

    private static final String FINAL_ORDER_PACK_LETTER_TYPE = "final-order-document-pack";
    private static final String TEST = "test";
    private static final String UPLOAD_TIMESTAMP = "14 Apr 2024 00:00:00";
    private static final Document DOCUMENT_LINK = new Document("document/url", TEST, TEST, TEST, TEST, UPLOAD_TIMESTAMP);
    private static final byte[] LETTER_CONTENT = new byte[]{37, 80, 68, 70, 45, 49, 46, 53, 10, 37, -61, -92};
    private static final String BEARER_TOKEN = "BEARER_TOKEN";

    private CaseData buildCaseData() {
        CaseData caseData = CaseData.builder()
            .parentClaimantIsApplicant(YesOrNo.YES)
            .claimant1PartyName("claimant1")
            .defendant1PartyName("defendant1")
            .ccdCaseReference(12345L)
            .build();

        return caseData;
    }

    private CaseData buildCivilCaseData() {
        CaseData caseData = CaseData.builder()
            .parentClaimantIsApplicant(YesOrNo.YES)
            .legacyCaseReference("00MC2")
            .applicant1(Party.builder()
                            .primaryAddress(Address.builder()
                                                .postCode("postcode")
                                                .postTown("posttown")
                                                .addressLine1("address1")
                                                .addressLine2("address2")
                                                .addressLine3("address3").build())
                            .partyName("applicant1partyname").build())
            .respondent1(Party.builder()
                             .primaryAddress(Address.builder()
                                                 .postCode("respondent1postcode")
                                                 .postTown("respondent1posttown")
                                                 .addressLine1("respondent1address1")
                                                 .addressLine2("respondent1address2")
                                                 .addressLine3("respondent1address3").build())
                             .partyName("respondent1partyname").build())
            .build();

        return caseData;
    }

    @Test
    void shouldDownloadDocumentAndPrintLetterSuccessfully() {
        // given
        Document document = Document.builder().documentUrl("url").documentFileName("filename").documentHash("hash")
            .documentBinaryUrl("binaryUrl").build();

        Party applicant = Party.builder()
            .primaryAddress(Address.builder()
                                .postCode("postcode")
                                .postTown("posttown")
                                .addressLine1("address1")
                                .addressLine2("address2")
                                .addressLine3("address3").build())
            .partyName("applicant1partyname").build();

        CaseData caseData = buildCaseData();
        CaseData civilCaseData = buildCivilCaseData();
        given(documentDownloadService.downloadDocument(any(), any()))
            .willReturn(new DownloadedDocumentResponse(new ByteArrayResource(LETTER_CONTENT), "test", "test"));

        // when
        sendFinalOrderPrintService.sendJudgeFinalOrderToPrintForLIP(BEARER_TOKEN, document, caseData, civilCaseData, FlowFlag.POST_JUDGE_ORDER_LIP_APPLICANT);

        // then
        verifyPrintLetter(civilCaseData, caseData, applicant);
    }

    @Test
    void shouldDownloadDocumentAndPrintLetterSuccessfullyRespondent() {
        // given
        Document document = Document.builder().documentUrl("url").documentFileName("filename").documentHash("hash")
            .documentBinaryUrl("binaryUrl").build();

        Party respondent = Party.builder()
            .primaryAddress(Address.builder()
                                .postCode("respondent1postcode")
                                .postTown("respondent1posttown")
                                .addressLine1("respondent1address1")
                                .addressLine2("respondent1address2")
                                .addressLine3("respondent1address3").build())
            .partyName("respondent1partyname").build();

        CaseData caseData = buildCaseData();
        CaseData civilCaseData = buildCivilCaseData();
        given(documentDownloadService.downloadDocument(any(), any()))
            .willReturn(new DownloadedDocumentResponse(new ByteArrayResource(LETTER_CONTENT), "test", "test"));

        // when
        sendFinalOrderPrintService.sendJudgeFinalOrderToPrintForLIP(BEARER_TOKEN, document, caseData, civilCaseData, FlowFlag.POST_JUDGE_ORDER_LIP_RESPONDENT);

        // then
        verifyPrintLetter(civilCaseData, caseData, respondent);
    }

    private void verifyPrintLetter(CaseData civilCaseData, CaseData caseData, Party party) {
        verify(bulkPrintService).printLetter(
            LETTER_CONTENT,
            caseData.getCcdCaseReference().toString(),
            civilCaseData.getLegacyCaseReference(),
            FINAL_ORDER_PACK_LETTER_TYPE,
            List.of(party.getPartyName())
        );
    }

}
