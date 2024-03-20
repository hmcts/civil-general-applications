package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_ADDITIONAL_INFO;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_DIRECTIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    RespondToJudgeAddlnInfoHandler.class,
    CaseDetailsConverter.class,
    JacksonAutoConfiguration.class},
    properties = {"reference.database.enabled=false"})
public class RespondToJudgeAddlnInfoHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    RespondToJudgeAddlnInfoHandler handler;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CaseDetailsConverter caseDetailsConverter;
    @MockBean
    IdamClient idamClient;

    private static final String CAMUNDA_EVENT = "INITIATE_GENERAL_APPLICATION";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    private static final String TEST_STRING = "anyValue";
    private static final String DUMMY_EMAIL = "test@gmail.com";
    private static final String APP_UID = "9";

    @BeforeEach
    public void setUp() throws IOException {
        when(idamClient.getUserInfo(anyString())).thenReturn(UserInfo.builder()
                .sub(DUMMY_EMAIL)
                .uid(APP_UID)
                .build());
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(RESPOND_TO_JUDGE_ADDITIONAL_INFO);
    }

    @Test
    void shouldPopulateDocListAndReturnNullWrittenRepUpload() {

        List<Element<Document>> generalAppAddlnInfoUpload = new ArrayList<>();

        Document document1 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        Document document2 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        generalAppAddlnInfoUpload.add(element(document1));
        generalAppAddlnInfoUpload.add(element(document2));

        CaseData caseData = getCase(generalAppAddlnInfoUpload, null);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        var responseCaseData = getCaseData(response);
        assertThat(response).isNotNull();
        assertThat(responseCaseData.getGeneralAppAddlnInfoUpload()).isEqualTo(null);
        assertThat(responseCaseData.getGaAddlDoc().size()).isEqualTo(2);
        assertThat(responseCaseData.getGaAddlDocStaff().size()).isEqualTo(2);
        assertThat(responseCaseData.getGaAddlDocClaimant().size()).isEqualTo(2);
    }

    @Test
    void shouldPopulateDocListWithExitingDocElement() {

        List<Element<Document>> generalAppAddlnInfoUpload = new ArrayList<>();

        Document document1 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        Document document2 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        generalAppAddlnInfoUpload.add(element(document1));
        generalAppAddlnInfoUpload.add(element(document2));

        List<Element<Document>> gaAddlnInfoList = new ArrayList<>();

        gaAddlnInfoList.add(element(document1));
        gaAddlnInfoList.add(element(document2));

        CaseData caseData = getCase(generalAppAddlnInfoUpload,
                DocUploadUtils.prepareDocuments(gaAddlnInfoList, DocUploadUtils.APPLICANT, RESPOND_TO_JUDGE_ADDITIONAL_INFO));

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        var responseCaseData = getCaseData(response);
        assertThat(response).isNotNull();
        assertThat(responseCaseData.getGeneralAppAddlnInfoUpload()).isEqualTo(null);
        assertThat(responseCaseData.getGaAddlDoc().size()).isEqualTo(4);
        assertThat(responseCaseData.getGaAddlDocStaff().size()).isEqualTo(2);
        assertThat(responseCaseData.getGaAddlDocClaimant().size()).isEqualTo(2);
    }

    private CaseData getCaseData(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData;
    }

    private CaseData getCase(List<Element<Document>> generalAppAddlnInfoUpload,
                             List<Element<CaseDocument>> gaAddlDoc) {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder().parentClaimantIsApplicant(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                        .email("abc@gmail.com").id(APP_UID).build())
            .generalAppAddlnInfoUpload(generalAppAddlnInfoUpload)
            .gaAddlDoc(gaAddlDoc)
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .generalAppType(
                GAApplicationType
                    .builder()
                    .types(types).build())
            .businessProcess(BusinessProcess
                                 .builder()
                                 .camundaEvent(CAMUNDA_EVENT)
                                 .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                                 .status(BusinessProcessStatus.READY)
                                 .activityId(ACTIVITY_ID)
                                 .build())
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .build();
    }
}
