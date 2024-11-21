package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.handler.event.UpdateFromGACaseEventHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.UploadDocumentByType;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.ADD_PDF_TO_MAIN_CASE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPLOAD_ADDL_DOCUMENTS;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder.STRING_CONSTANT;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@ExtendWith(MockitoExtension.class)
class UploadAdditionalDocumentsCallbackHandlerTest extends BaseCallbackHandlerTest {

    UploadAdditionalDocumentsCallbackHandler handler;

    ObjectMapper objectMapper;

    @Mock
    IdamClient idamClient;

    @Mock
    CaseDetailsConverter caseDetailsConverter;

    @Mock
    UpdateFromGACaseEventHandler updateFromGACaseEventHandler;

    @Mock
    AssignCategoryId assignCategoryId;

    private static final String DUMMY_EMAIL = "test@gmail.com";

    @BeforeEach
    public void setUp() throws IOException {

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new UploadAdditionalDocumentsCallbackHandler(objectMapper, assignCategoryId, caseDetailsConverter, idamClient, updateFromGACaseEventHandler);
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(UPLOAD_ADDL_DOCUMENTS);
    }

    @Nested
    class AboutToSubmit {

        @BeforeEach
        public void setUp() throws IOException {

            when(idamClient.getUserInfo(anyString())).thenReturn(UserInfo.builder()
                    .sub(DUMMY_EMAIL)
                    .uid(STRING_CONSTANT)
                    .build());

        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenJudgeUncloaked() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("Witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .applicationIsUncloakedOnce(YES)
                .parentClaimantIsApplicant(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id(STRING_CONSTANT).forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenJudgeCloakedApplication() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .applicationIsCloaked(NO)
                .parentClaimantIsApplicant(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id(STRING_CONSTANT).forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenJudgeIsNotUncloakedAndInformOtherPartyIsYes() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .parentClaimantIsApplicant(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id(STRING_CONSTANT).forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenApplicationIsUrgent() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));
            List<Element<GASolicitorDetailsGAspec>> gaApplAddlSolicitors = new ArrayList<>();
            gaApplAddlSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                                 .id("id1")
                                                 .email(DUMMY_EMAIL)
                                                 .organisationIdentifier("1").build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .parentClaimantIsApplicant(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id(STRING_CONSTANT).forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .generalAppApplicantAddlSolicitors(gaApplAddlSolicitors)
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(NO);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenApplicationIsUrgentAddedToApplicantAddlUser() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));
            List<Element<GASolicitorDetailsGAspec>> gaApplAddlSolicitors = new ArrayList<>();
            gaApplAddlSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                                 .id(STRING_CONSTANT)
                                                 .email(DUMMY_EMAIL)
                                                 .organisationIdentifier("1").build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .parentClaimantIsApplicant(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id1").forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .generalAppApplicantAddlSolicitors(gaApplAddlSolicitors)
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(NO);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenApplicationIsNonUrgentAndAddedToRespCollection() {

            List<Element<UploadDocumentByType>> uploadDocumentByRespondent = new ArrayList<>();
            uploadDocumentByRespondent.add(element(UploadDocumentByType.builder()
                                                       .documentType("witness")
                                                       .additionalDocument(Document.builder()
                                                                               .documentFileName("witness_document.pdf")
                                                                               .documentUrl("http://dm-store:8080")
                                                                               .documentBinaryUrl("http://dm-store:8080/documents")
                                                                               .build()).build()));
            List<Element<GASolicitorDetailsGAspec>> gaRespSolicitors = new ArrayList<>();
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id(STRING_CONSTANT)
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .isMultiParty(NO)
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                .parentClaimantIsApplicant(NO)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id").forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByRespondent)
                .generalAppRespondentSolicitors(gaRespSolicitors)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
            assertThat(responseCaseData.getGaAddlDocRespondentSol().size()).isEqualTo(1);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenApplicationIsConsentOrderAndAddedToResp1Collection1v2() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));

            List<Element<GASolicitorDetailsGAspec>> gaRespSolicitors = new ArrayList<>();
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("id11")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id(STRING_CONSTANT)
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("id3")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("222")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("3").build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .generalAppConsentOrder(YES)
                .generalAppRespondentSolicitors(gaRespSolicitors)
                .parentClaimantIsApplicant(NO)
                .isMultiParty(NO)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("2").forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
            assertThat(responseCaseData.getGaAddlDocRespondentSol().size()).isEqualTo(1);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenApplicationIsConsentOrderAndAddedToResp2Collection1v2() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));

            List<Element<GASolicitorDetailsGAspec>> gaRespSolicitors = new ArrayList<>();
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("222")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("id1")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("id3")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id(STRING_CONSTANT)
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("3").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("id33")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("3").build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .generalAppConsentOrder(YES)
                .generalAppRespondentSolicitors(gaRespSolicitors)
                .parentClaimantIsApplicant(NO)
                .isMultiParty(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("2").forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
            assertThat(responseCaseData.getGaAddlDocRespondentSolTwo().size()).isEqualTo(1);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenApplicationIsConsentOrderAndAddedToResp2Collection1v2MultipleCollection() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));
            List<Element<CaseDocument>> documentsCollection = new ArrayList<>();
            documentsCollection.add(element(CaseDocument.builder().createdBy("civil")
                                                .documentLink(Document.builder()
                                                                  .documentFileName("witness_document.pdf")
                                                                  .documentUrl("http://dm-store:8080")
                                                                  .documentBinaryUrl("http://dm-store:8080/documents")
                                                                  .build())
                                                .documentName("witness_document.docx")
                                                .createdDatetime(now()).build()));

            List<Element<GASolicitorDetailsGAspec>> gaRespSolicitors = new ArrayList<>();
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("222")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id(STRING_CONSTANT)
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("3").build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .generalAppConsentOrder(YES)
                .generalAppRespondentSolicitors(gaRespSolicitors)
                .parentClaimantIsApplicant(NO)
                .gaAddlDocStaff(documentsCollection)
                .gaAddlDoc(documentsCollection)
                .gaAddlDocRespondentSolTwo(documentsCollection)
                .isMultiParty(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("2").forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
            assertThat(responseCaseData.getGaAddlDocRespondentSolTwo().size()).isEqualTo(2);
        }

        @Test
        void shouldSetUpReadyBusinessProcessWhenApplicationIsNotConsentOrder() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .parentClaimantIsApplicant(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id(STRING_CONSTANT).forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(NO);
        }

        @Test
        void shouldPutBundleInBundleCollection() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("bundle")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl("http://dm-store:8080/documents")
                                                                              .build()).build()));

            List<Element<GASolicitorDetailsGAspec>> gaRespSolicitors = new ArrayList<>();
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id("222")
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id(STRING_CONSTANT)
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("3").build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .generalAppConsentOrder(YES)
                .generalAppRespondentSolicitors(gaRespSolicitors)
                .parentClaimantIsApplicant(NO)
                .isMultiParty(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("2").forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getIsDocumentVisible()).isEqualTo(YES);
            assertThat(responseCaseData.getGaAddlDocRespondentSolTwo()).isNull();
            assertThat(responseCaseData.getGaAddlDocBundle().size()).isEqualTo(1);
        }
    }

    @Nested
    class SubmittedCallback {
        @Test
        void shouldReturnExpectedSubmittedCallbackResponse_whenInvoked() {
            String body = "<br/> <br/>";
            String header = "### File has been uploaded successfully.";
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(NO)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();

            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
            doNothing().when(updateFromGACaseEventHandler).handleEventUpdate(params, ADD_PDF_TO_MAIN_CASE);

            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).usingRecursiveComparison().isEqualTo(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader(format(header))
                    .confirmationBody(format(body))
                    .build());
        }
    }
}
