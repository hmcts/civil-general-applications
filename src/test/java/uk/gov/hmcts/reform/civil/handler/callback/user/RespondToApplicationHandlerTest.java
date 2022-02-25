package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@SpringBootTest(classes = {
    RespondToApplicationHandler.class,
    JacksonAutoConfiguration.class,
},
    properties = {"reference.database.enabled=false"})
public class RespondToApplicationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    RespondToApplicationHandler handler;

    private static final String CAMUNDA_EVENT = "INITIATE_GENERAL_APPLICATION";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    private static final String CONFIRMATION_MESSAGE = "<br/><p> You have responded to the following application: </p>"
        + "<ul> <li>Summary judgment</li> </ul>"
        + " <p> The application and your response will be reviewed by a Judge </p> ";

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(RESPOND_TO_APPLICATION);
    }

    @Test
    void buildResponseConfirmationReturnsCorrectMessage() {
        CallbackParams params = callbackParamsOf(getCase(), CallbackType.SUBMITTED);
        var response = (SubmittedCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getConfirmationBody()).isEqualTo(CONFIRMATION_MESSAGE);
    }

    private CaseData getCase() {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .generalAppType(
                    GAApplicationType
                        .builder()
                        .types(types).build())
                .businessProcess(BusinessProcess
                                     .builder()
                                     .camundaEvent(CAMUNDA_EVENT)
                                     .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                                     .status(BusinessProcessStatus.STARTED)
                                     .activityId(ACTIVITY_ID)
                                     .build()).build();
    }
}
