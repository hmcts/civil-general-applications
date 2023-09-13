package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    RespondToWrittenRepresentationHandler.class,
    CaseDetailsConverter.class,
    JacksonAutoConfiguration.class},
    properties = {"reference.database.enabled=false"})
public class RespondWrittenRepresentationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    RespondToWrittenRepresentationHandler handler;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CaseDetailsConverter caseDetailsConverter;
    @MockBean
    AssignCategoryId assignCategoryId;
    private static final String CAMUNDA_EVENT = "INITIATE_GENERAL_APPLICATION";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    private static final String TEST_STRING = "anyValue";

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION);
    }

    @Test
    void shouldPopulateDocListAndReturnNullWrittenRepUpload() {

        List<Element<Document>> generalAppWrittenRepUpload = new ArrayList<>();
        List<Element<Document>> gaWrittenRepDocList = new ArrayList<>();

        Document document1 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        Document document2 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        generalAppWrittenRepUpload.add(element(document1));
        generalAppWrittenRepUpload.add(element(document2));

        CaseData caseData = getCase(generalAppWrittenRepUpload, gaWrittenRepDocList);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        var responseCaseData = getCaseData(response);
        assertThat(response).isNotNull();
        assertThat(responseCaseData.getGeneralAppWrittenRepUpload()).isEqualTo(null);
        assertThat(responseCaseData.getGaWrittenRepDocList().size()).isEqualTo(2);
        assertThat(responseCaseData.getGaRespDocument().size()).isEqualTo(2);

    }

    @Test
    void shouldPopulateDocListWithExitingDocElement() {

        List<Element<Document>> generalAppWrittenRepUpload = new ArrayList<>();

        Document document1 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        Document document2 = Document.builder().documentFileName(TEST_STRING).documentUrl(TEST_STRING)
            .documentBinaryUrl(TEST_STRING)
            .documentHash(TEST_STRING).build();

        generalAppWrittenRepUpload.add(element(document1));
        generalAppWrittenRepUpload.add(element(document2));

        List<Element<Document>> gaWrittenRepDocList = new ArrayList<>();

        gaWrittenRepDocList.add(element(document1));
        gaWrittenRepDocList.add(element(document2));

        CaseData caseData = getCase(generalAppWrittenRepUpload, gaWrittenRepDocList);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        var responseCaseData = getCaseData(response);
        assertThat(response).isNotNull();
        assertThat(responseCaseData.getGeneralAppWrittenRepUpload()).isEqualTo(null);
        assertThat(responseCaseData.getGaWrittenRepDocList().size()).isEqualTo(4);
        assertThat(responseCaseData.getGaRespDocument().size()).isEqualTo(4);
    }

    private CaseData getCaseData(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData;
    }

    private CaseData getCase(List<Element<Document>> generalAppWrittenRepUpload,
                             List<Element<Document>> gaWrittenRepDocList) {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
            .generalAppWrittenRepUpload(generalAppWrittenRepUpload)
            .gaWrittenRepDocList(gaWrittenRepDocList)
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
            .ccdState(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .build();
    }

}
