package uk.gov.hmcts.reform.civil.service.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.civil.client.DocmosisApiClient;
import uk.gov.hmcts.reform.civil.config.DocmosisConfiguration;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisRequest;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.GENERAL_ORDER;

@ExtendWith(MockitoExtension.class)
class DocumentGeneratorServiceTest {

    @Mock
    private DocmosisApiClient docmosisApiClient;

    @Captor
    ArgumentCaptor<DocmosisRequest> argumentCaptor;

    @InjectMocks
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private DocmosisConfiguration configuration;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldInvokeTornado() {
        JudgeDecisionPdfDocument judgeDecisionPdfDocument = JudgeDecisionPdfDocument.builder().build();

        byte[] expectedResponse = {1, 2, 3};
        when(docmosisApiClient.createDocument(argumentCaptor.capture())).thenReturn(expectedResponse);

        DocmosisDocument docmosisDocument = documentGeneratorService
            .generateDocmosisDocument(judgeDecisionPdfDocument, GENERAL_ORDER);
        assertThat(docmosisDocument.getBytes()).isEqualTo(expectedResponse);

        assertThat(argumentCaptor.getValue().getTemplateName()).isEqualTo(GENERAL_ORDER.getTemplate());
        assertThat(argumentCaptor.getValue().getOutputFormat()).isEqualTo("pdf");
    }

    @Test
    void shouldThrowWhenTornadoFails() {
        when(docmosisApiClient.createDocument(argumentCaptor.capture()))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "not found"));

        Map<String, Object> placeholders = Map.of();

        HttpClientErrorException httpClientErrorException = assertThrows(
            HttpClientErrorException.class,
            () -> documentGeneratorService.generateDocmosisDocument(placeholders, GENERAL_ORDER)
        );

        assertThat(httpClientErrorException).hasMessageContaining("404 not found");
    }
}

