package uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.dismissalorder.DismissalOrder;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.DISMISSAL_ORDER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DismissalOrderGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class DismissalOrderGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final String REFERENCE_NUMBER = "000DC001";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};
    private static final String fileName = format(DISMISSAL_ORDER.getDocumentTitle(), REFERENCE_NUMBER);
    private static final CaseDocument CASE_DOCUMENT = CaseDocument.builder()
        .documentName(fileName)
        .documentType(DocumentType.DISMISSAL_ORDER)
        .build();

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @Autowired
    private DismissalOrderGenerator dismissalOrderGenerator;

    @Test
    void shouldGenerateDismissalOrderDocument() {
        CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(DISMISSAL_ORDER)))
            .thenReturn(new DocmosisDocument(DISMISSAL_ORDER.getDocumentTitle(), bytes));

        when(documentManagementService.uploadDocument(
            BEARER_TOKEN,
            new PDF(fileName, bytes, DocumentType.DISMISSAL_ORDER)
        ))
            .thenReturn(CASE_DOCUMENT);

        CaseDocument caseDocument = dismissalOrderGenerator.generate(caseData, BEARER_TOKEN);
        assertThat(caseDocument).isNotNull().isEqualTo(CASE_DOCUMENT);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(fileName, bytes, DocumentType.DISMISSAL_ORDER)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(DismissalOrder.class), eq(DISMISSAL_ORDER));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetDissmisalOrderData() {
            CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication().build().toBuilder()
                .build();

            var templateData = dismissalOrderGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_DismissalOrder(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_DismissalOrder(DismissalOrder templateData, CaseData caseData) {
            Assertions.assertAll(
                "Dismissal Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getSubmittedOn(), caseData.getSubmittedOn()),
                () -> assertEquals(templateData.getApplicantName(), caseData.getApplicantPartyName()),
                () -> assertEquals(templateData.getApplicationDate(), caseData.getCreatedDate().toLocalDate()),
                () -> assertEquals(templateData.getDismissalOrder(),
                                   caseData.getJudicialDecisionMakeOrder().getDismissalOrderText()),
                () -> assertEquals(templateData.getReasonForDecision(),
                                   caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText())
            );
        }

        private String getClaimats(CaseData caseData) {
            List<String> claimantsName = new ArrayList<>();
            claimantsName.add(caseData.getClaimant1PartyName());
            if (caseData.getDefendant2PartyName() != null) {
                claimantsName.add(caseData.getClaimant2PartyName());
            }
            return String.join(", ", claimantsName);
        }

        private String getDefendats(CaseData caseData) {
            List<String> defendatsName = new ArrayList<>();
            defendatsName.add(caseData.getDefendant1PartyName());
            if (caseData.getDefendant2PartyName() != null) {
                defendatsName.add(caseData.getDefendant2PartyName());
            }
            return String.join(", ", defendatsName);
        }

        private String getApplicationType(CaseData caseData) {
            List<GeneralApplicationTypes> types = caseData.getGeneralAppType().getTypes();
            return types.stream()
                .map(GeneralApplicationTypes::getDisplayedValue).collect(Collectors.joining(", "));
        }
    }
}
