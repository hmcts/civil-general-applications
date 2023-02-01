package uk.gov.hmcts.reform.civil.service.docmosis.hearingorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingNoticeDetail;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDocumentBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.HEARING_FORM;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.HEARING_APPLICATION;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        HearingFormGenerator.class,
        JacksonAutoConfiguration.class
})
class HearingFormGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final String REFERENCE_NUMBER = "000DC001";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};
    private static final String fileName_application = "Application_Hearing_Notice_"
            + DateFormatHelper.formatLocalDate(LocalDate.now(), "ddMMyyyy")
            + ".pdf";
    private static final CaseDocument CASE_DOCUMENT = CaseDocumentBuilder.builder()
            .documentName(fileName_application)
            .documentType(HEARING_FORM)
            .build();

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private HearingFormGenerator generator;

    @Test
    void shouldHearingFormGeneratorOneForm_whenValidDataIsProvided() {
        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(HEARING_APPLICATION)))
                .thenReturn(new DocmosisDocument(HEARING_APPLICATION.getDocumentTitle(), bytes));

        when(documentManagementService
                .uploadDocument(BEARER_TOKEN, new PDF(fileName_application, bytes, HEARING_FORM)))
                .thenReturn(CASE_DOCUMENT);

        Map<String, String> refMap = new HashMap<>();
        refMap.put("applicantSolicitor1Reference", "app1ref");
        refMap.put("respondentSolicitor1Reference", "resp1ref");
        refMap.put("respondentSolicitor2Reference", "resp2ref");
        Map<String, Object> caseDataContent = new HashMap<>();
        caseDataContent.put("solicitorReferences", refMap);
        CaseDetails caseDetails = CaseDetails.builder().data(caseDataContent).build();
        when(coreCaseDataService.getCase(
                anyLong()
        )).thenReturn(caseDetails);

        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YES).build();
        CaseDocument caseDocuments = generator.generate(caseData, BEARER_TOKEN);

        assertThat(caseDocuments).isNotNull();

        verify(documentManagementService)
                .uploadDocument(BEARER_TOKEN, new PDF(fileName_application, bytes, HEARING_FORM));
    }

    @Test
    void test_getCaseNumberFormatted() {
        CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(1644495739087775L).build();
        String formattedCaseNumber = generator.getCaseNumberFormatted(caseData);
        assertThat(formattedCaseNumber).isEqualTo("1644-4957-3908-7775");
    }

    @Test
    void test_getFileName() {
        String name = generator.getFileName(null, HEARING_APPLICATION);
        assertThat(name).startsWith("Application_Hearing_Notice_");
        assertThat(name).endsWith(".pdf");
    }

    @Test
    void test_getDateFormatted() {
        String dateString = generator.getDateFormatted(LocalDate.EPOCH);
        assertThat(dateString).isEqualTo("01/Jan/1970");
    }

    @Test
    void test_getReference() {
        Map<String, String> refMap = new HashMap<>();
        refMap.put("applicantSolicitor1Reference", "app1ref");
        refMap.put("respondentSolicitor1Reference", "resp1ref");
        refMap.put("respondentSolicitor2Reference", "resp2ref");
        Map<String, Object> caseDataContent = new HashMap<>();
        caseDataContent.put("solicitorReferences", refMap);
        CaseDetails caseDetails = CaseDetails.builder().data(caseDataContent).build();

        assertThat(generator.getReference(caseDetails, "applicantSolicitor1Reference")).isEqualTo("app1ref");
        assertThat(generator.getReference(caseDetails, "notExist")).isNull();
    }

    @Test
    void test_getHearingTimeFormatted() {
        assertThat(HearingFormGenerator.getHearingTimeFormatted("error")).isNull();
        assertThat(HearingFormGenerator.getHearingTimeFormatted("0800")).isEqualTo("08:00");
    }

    @Test
    void test_getHearingDurationString() {
        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YES).build();
        String durationString = HearingFormGenerator.getHearingDurationString(caseData);
        assertThat(durationString).isEqualTo(GAHearingDuration.HOUR_1.getDisplayedValue());
    }

    @Test
    void test_getHearingDurationStringOther() {
        CaseData caseData = CaseData.builder().gaHearingNoticeDetail(GAHearingNoticeDetail.builder()
                .hearingDuration(GAHearingDuration.OTHER)
                .hearingDurationOther("One year").build()).build();
        String durationString = HearingFormGenerator.getHearingDurationString(caseData);
        assertThat(durationString).isEqualTo("One year");
    }

    @Test
    void test_getTemplate() {
        CaseData caseData = CaseDataBuilder.builder().build();
        assertThat(generator.getTemplate(caseData)).isEqualTo(HEARING_APPLICATION);
    }
}