package uk.gov.hmcts.reform.civil.service.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.civil.client.DocmosisApiClient;
import uk.gov.hmcts.reform.civil.config.DocmosisConfiguration;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisRequest;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentGeneratorService {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private final DocmosisApiClient docmosisApiClient;
    private final DocmosisConfiguration configuration;
    private final ObjectMapper mapper;

    public DocmosisDocument generateDocmosisDocument(MappableObject templateData, DocmosisTemplates template) {
        return generateDocmosisDocument(templateData.toMap(mapper), template);
    }

    public DocmosisDocument generateDocmosisDocument(Map<String, Object> templateData, DocmosisTemplates template) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("Generating docmosis document for template: {}", template.getTemplate());
        DocmosisRequest requestBody = DocmosisRequest.builder()
            .templateName(template.getTemplate())
            .data(templateData)
            .outputFormat("pdf")
            .outputName("IGNORED")
            .accessKey(configuration.getAccessKey())
            .build();

        byte[] response;

        try {
            response = docmosisApiClient.createDocument(requestBody);
        } catch (HttpClientErrorException ex) {
            log.error("Docmosis document generation failed for " + ex.getMessage());
            throw ex;
        }

        return new DocmosisDocument(template.getDocumentTitle(), response);
    }
}
