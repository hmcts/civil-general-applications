package uk.gov.hmcts.reform.civil.service.docmosis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.civil.config.DocmosisConfiguration;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisRequest;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.API_RENDER;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DocumentGeneratorService.class, JacksonAutoConfiguration.class})
class DocumentGeneratorServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<byte[]> tornadoResponse;

    @MockBean
    private DocmosisConfiguration configuration;

    @Captor
    ArgumentCaptor<HttpEntity<DocmosisRequest>> argumentCaptor;

    @Autowired
    private DocumentGeneratorService documentGeneratorService;

    @Test
    void shouldInvokesTornado() {
        JudgeDecisionPdfDocument judgeDecisionPdfDocument = JudgeDecisionPdfDocument.builder().build();

        when(restTemplate.exchange(eq(configuration.getUrl() + API_RENDER),
                                   eq(HttpMethod.POST), argumentCaptor.capture(), eq(byte[].class)
        )).thenReturn(tornadoResponse);

        byte[] expectedResponse = {1, 2, 3};
        when(tornadoResponse.getBody()).thenReturn(expectedResponse);

        DocmosisDocument docmosisDocument = documentGeneratorService
            .generateDocmosisDocument(judgeDecisionPdfDocument, GENERAL_ORDER);
        assertThat(docmosisDocument.getBytes()).isEqualTo(expectedResponse);

        assertThat(argumentCaptor.getValue().getBody().getTemplateName()).isEqualTo(GENERAL_ORDER.getTemplate());
        assertThat(argumentCaptor.getValue().getBody().getOutputFormat()).isEqualTo("pdf");
    }

    @Test
    void shouldThrowWhenTornadoFails() {
        when(restTemplate.exchange(eq(configuration.getUrl() + API_RENDER),
                                   eq(HttpMethod.POST), argumentCaptor.capture(), eq(byte[].class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "not found"));

        Map<String, Object> placeholders = Map.of();

        HttpClientErrorException httpClientErrorException = assertThrows(
            HttpClientErrorException.class,
            () -> documentGeneratorService.generateDocmosisDocument(placeholders, GENERAL_ORDER)
        );

        assertThat(httpClientErrorException).hasMessageContaining("404 not found");
    }
}

