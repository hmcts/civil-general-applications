package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.civil.config.DocumentManagementConfiguration;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadException;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentUploadException;
import uk.gov.hmcts.reform.civil.service.documentmanagement.SecuredDocumentManagementService;
import uk.gov.hmcts.reform.civil.utils.ResourceReader;
import uk.gov.hmcts.reform.document.DocumentDownloadClientApi;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentDownloadException.MESSAGE_TEMPLATE;
import static uk.gov.hmcts.reform.civil.service.documentmanagement.SecuredDocumentManagementService.DOC_UUID_LENGTH;

@SpringBootTest(classes = {
    SecuredDocumentManagementService.class,
    JacksonAutoConfiguration.class,
    DocumentManagementConfiguration.class})
public class SecuredDocumentManagementServiceTest {

    private static final String USER_ROLES_JOINED = "caseworker-civil,caseworker-civil-solicitor";
    public static final String BEARER_TOKEN = "Bearer Token";

    @MockBean
    private CaseDocumentClientApi caseDocumentClientApi;
    @MockBean
    private DocumentDownloadClientApi documentDownloadClient;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SecuredDocumentManagementService documentManagementService;

    @Mock
    private ResponseEntity<Resource> responseEntity;
    private final UserInfo userInfo = UserInfo.builder()
        .roles(List.of("role"))
        .uid("id")
        .givenName("userFirstName")
        .familyName("userLastName")
        .sub("mail@mail.com")
        .build();

    @BeforeEach
    public void setUp() {
        when(authTokenGenerator.generate()).thenReturn(BEARER_TOKEN);
        when(userService.getUserInfo(anyString())).thenReturn(userInfo);
    }

    @Nested
    class UploadDocument {

        @Test
        void shouldUploadToDocumentManagement() throws JsonProcessingException {
            PDF document = new PDF("0000-claim.pdf", "test".getBytes(), GENERAL_ORDER);

            UploadResponse uploadResponse = mapper.readValue(
                ResourceReader.readString("document-management/secured.response.success.json"),
                UploadResponse.class
            );

            when(caseDocumentClientApi.uploadDocuments(anyString(), anyString(), any(DocumentUploadRequest.class)))
                .thenReturn(uploadResponse);

            CaseDocument caseDocument = documentManagementService.uploadDocument(BEARER_TOKEN, document);
            assertNotNull(caseDocument.getDocumentLink());
            Assertions.assertEquals(
                uploadResponse.getDocuments().get(0).links.self.href,
                caseDocument.getDocumentLink().getDocumentUrl()
            );

            verify(caseDocumentClientApi).uploadDocuments(anyString(), anyString(), any(DocumentUploadRequest.class));
        }

        @Test
        void shouldThrow_whenUploadDocumentFails() throws JsonProcessingException {
            PDF document = new PDF("0000-failed-claim.pdf", "failed-test".getBytes(), GENERAL_ORDER);

            UploadResponse uploadResponse = mapper.readValue(
                ResourceReader.readString("document-management/secured.response.failure.json"),
                UploadResponse.class
            );

            when(caseDocumentClientApi.uploadDocuments(anyString(), anyString(), any(DocumentUploadRequest.class)))
                .thenReturn(uploadResponse);

            DocumentUploadException documentManagementException = assertThrows(
                DocumentUploadException.class,
                () -> documentManagementService.uploadDocument(BEARER_TOKEN, document)
            );

            assertEquals(
                "Unable to upload document 0000-failed-claim.pdf to document management.",
                documentManagementException.getMessage()
            );

            verify(caseDocumentClientApi).uploadDocuments(anyString(), anyString(), any(DocumentUploadRequest.class));
        }
    }

    @Nested
    class DownloadDocument {

        @Test
        void shouldDownloadDocumentFromDocumentManagement() throws JsonProcessingException {

            Document document = mapper.readValue(
                ResourceReader.readString("document-management/download.success.json"),
                Document.class
            );
            String documentPath = URI.create(document.links.self.href).getPath();
            String documentBinary = URI.create(document.links.binary.href).getPath().replaceFirst("/", "");
            UUID documentId = getDocumentIdFromSelfHref(documentPath);

            when(caseDocumentClientApi.getMetadataForDocument(
                     anyString(),
                     anyString(),
                     eq(documentId)
                 )
            ).thenReturn(document);

            when(responseEntity.getBody()).thenReturn(new ByteArrayResource("test".getBytes()));

            when(documentDownloadClient.downloadBinary(
                     anyString(),
                     anyString(),
                     eq(USER_ROLES_JOINED),
                     anyString(),
                     eq(documentBinary)
                 )
            ).thenReturn(responseEntity);

            byte[] pdf = documentManagementService.downloadDocument(BEARER_TOKEN, documentPath);

            assertNotNull(pdf);
            assertArrayEquals("test".getBytes(), pdf);

            verify(caseDocumentClientApi).getMetadataForDocument(anyString(), anyString(), eq(documentId));

            verify(documentDownloadClient)
                .downloadBinary(anyString(), anyString(), eq(USER_ROLES_JOINED), anyString(), eq(documentBinary));
        }

        @Test
        void shouldThrow_whenDocumentDownloadFails() throws JsonProcessingException {
            Document document = mapper.readValue(
                ResourceReader.readString("document-management/download.success.json"),
                Document.class
            );
            String documentPath = "/documents/85d97996-22a5-40d7-882e-3a382c8ae1b7";
            String documentBinary = "documents/85d97996-22a5-40d7-882e-3a382c8ae1b7/binary";
            UUID documentId = getDocumentIdFromSelfHref(documentPath);

            when(caseDocumentClientApi.getMetadataForDocument(
                     anyString(),
                     anyString(),
                     eq(documentId)
                 )
            ).thenReturn(document);

            when(documentDownloadClient
                     .downloadBinary(anyString(), anyString(), eq(USER_ROLES_JOINED), anyString(), eq(documentBinary))
            ).thenReturn(null);

            DocumentDownloadException documentManagementException = assertThrows(
                DocumentDownloadException.class,
                () -> documentManagementService.downloadDocument(BEARER_TOKEN, documentPath)
            );

            assertEquals(format(MESSAGE_TEMPLATE, documentPath), documentManagementException.getMessage());

            verify(caseDocumentClientApi).getMetadataForDocument(anyString(), anyString(), eq(documentId));
        }

        @Test
        void shouldDownloadDocumentByDocumentPathMetaData() throws JsonProcessingException {
            //Given
            Document document = mapper.readValue(
                ResourceReader.readString("document-management/download.success.json"),
                Document.class
            );
            String documentPath = "/documents/85d97996-22a5-40d7-882e-3a382c8ae1b7";
            UUID documentId = getDocumentIdFromSelfHref(documentPath);

            when(caseDocumentClientApi.getMetadataForDocument(
                     anyString(),
                     anyString(),
                     eq(documentId)
                 )
            ).thenReturn(document);

            when(caseDocumentClientApi.getDocumentBinary(
                     anyString(),
                     anyString(),
                     eq(documentId)
                 )
            ).thenReturn(responseEntity);

            when(responseEntity.getBody()).thenReturn(new ByteArrayResource("test".getBytes()));

            //When
            DownloadedDocumentResponse expectedResult =
                new DownloadedDocumentResponse(new ByteArrayResource("test".getBytes()), "TEST_DOCUMENT_1.pdf",
                                               "application/pdf");

            //Then
            assertEquals(expectedResult, documentManagementService.downloadDocumentWithMetaData(BEARER_TOKEN, documentPath));
        }

        @Test
        void shouldDownloadDocumentFromDocumentManagementIfNullCdam() throws JsonProcessingException {

            Document document = mapper.readValue(
                ResourceReader.readString("document-management/download.success.json"),
                Document.class
            );
            String documentPath = URI.create(document.links.self.href).getPath();
            String documentBinary = URI.create(document.links.binary.href).getPath().replaceFirst("/", "");
            UUID documentId = getDocumentIdFromSelfHref(documentPath);

            when(caseDocumentClientApi.getMetadataForDocument(
                     anyString(),
                     anyString(),
                     eq(documentId)
                 )
            ).thenReturn(document);

            when(responseEntity.getBody()).thenReturn(new ByteArrayResource("test".getBytes()));

            when(documentDownloadClient.downloadBinary(
                     anyString(),
                     anyString(),
                     eq(USER_ROLES_JOINED),
                     anyString(),
                     eq(documentBinary)
                 )
            ).thenReturn(responseEntity);

            DownloadedDocumentResponse expectedResult =
                new DownloadedDocumentResponse(new ByteArrayResource("test".getBytes()), "TEST_DOCUMENT_1.pdf",
                                               "application/pdf");

            assertEquals(expectedResult, documentManagementService.downloadDocumentWithMetaData(BEARER_TOKEN, documentPath));
        }

        @Test
        void shouldThrow_whenDocumentDownloadMetaDataFails() throws JsonProcessingException {
            Document document = mapper.readValue(
                ResourceReader.readString("document-management/download.success.json"),
                Document.class
            );
            String documentPath = "/documents/85d97996-22a5-40d7-882e-3a382c8ae1b7";
            String documentBinary = "documents/85d97996-22a5-40d7-882e-3a382c8ae1b7/binary";
            UUID documentId = getDocumentIdFromSelfHref(documentPath);

            when(caseDocumentClientApi.getMetadataForDocument(
                     anyString(),
                     anyString(),
                     eq(documentId)
                 )
            ).thenReturn(document);

            when(documentDownloadClient
                     .downloadBinary(anyString(), anyString(), eq(USER_ROLES_JOINED), anyString(), eq(documentBinary))
            ).thenReturn(null);

            DocumentDownloadException documentManagementException = assertThrows(
                DocumentDownloadException.class,
                () -> documentManagementService.downloadDocumentWithMetaData(BEARER_TOKEN, documentPath)
            );

            assertEquals(format(MESSAGE_TEMPLATE, documentPath), documentManagementException.getMessage());

            verify(caseDocumentClientApi).getMetadataForDocument(anyString(), anyString(), eq(documentId));
        }
    }

    @Nested
    class DocumentMetaData {
        @Test
        void getDocumentMetaData() throws JsonProcessingException {
            String documentPath = "/documents/85d97996-22a5-40d7-882e-3a382c8ae1b3";
            UUID documentId = getDocumentIdFromSelfHref(documentPath);

            when(caseDocumentClientApi.getMetadataForDocument(
                     anyString(),
                     anyString(),
                     eq(documentId)
                 )
            ).thenReturn(mapper.readValue(
                ResourceReader.readString("document-management/metadata.success.json"), Document.class)
            );

            when(responseEntity.getBody()).thenReturn(new ByteArrayResource("test".getBytes()));

            Document documentMetaData
                = documentManagementService.getDocumentMetaData(BEARER_TOKEN, documentPath);

            Assertions.assertEquals(72552L, documentMetaData.size);
            Assertions.assertEquals("TEST_DOCUMENT_1.pdf", documentMetaData.originalDocumentName);

            verify(caseDocumentClientApi).getMetadataForDocument(anyString(), anyString(), eq(documentId));
        }

        @Test
        void shouldThrow_whenMetadataDownloadFails() {
            String documentPath = "/documents/85d97996-22a5-40d7-882e-3a382c8ae1b5";
            UUID documentId = getDocumentIdFromSelfHref(documentPath);

            when(caseDocumentClientApi
                     .getMetadataForDocument(anyString(), anyString(), eq(documentId))
            ).thenThrow(new RuntimeException("Failed to access document metadata"));

            DocumentDownloadException documentManagementException = assertThrows(
                DocumentDownloadException.class,
                () -> documentManagementService.getDocumentMetaData(BEARER_TOKEN, documentPath)
            );

            assertEquals(
                String.format(MESSAGE_TEMPLATE, documentPath),
                documentManagementException.getMessage()
            );

            verify(caseDocumentClientApi)
                .getMetadataForDocument(anyString(), anyString(), eq(documentId));
        }
    }

    private UUID getDocumentIdFromSelfHref(String selfHref) {
        return UUID.fromString(selfHref.substring(selfHref.length() - DOC_UUID_LENGTH));
    }
}
