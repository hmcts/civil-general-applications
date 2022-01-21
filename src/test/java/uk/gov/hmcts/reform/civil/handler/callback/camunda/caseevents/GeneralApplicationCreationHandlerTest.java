package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseevents;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess.StartGeneralApplicationBusinessProcessCallbackHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_GENERAL_APPLICATION_CASE;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@SpringBootTest(classes = {
    GeneralApplicationCreationHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
},
    properties = {"reference.database.enabled=false"})
class GeneralApplicationCreationHandlerTest extends BaseCallbackHandlerTest {

    private static final String STRING_CONSTANT = "this is a string";
    private static final LocalDate APP_DATE_EPOCH = EPOCH;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private CallbackParams params;

    @Autowired
    private GeneralApplicationCreationHandler generalApplicationCreationHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class AboutToSubmitCallBack {

        @Test
        void testTheResponseIsNullWhenNothingIsPassed() {
            var response = (SubmittedCallbackResponse) generalApplicationCreationHandler.handle(params);
            assertThat(response).isNull();
        }

        @Test
        void checkStartGaUpdateStartsTheUpdateWithGivenParametersWhenInvoked() {
            CaseDetails caseDetails = CaseDetails.builder().id(1L).build();
            StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();
            assertThat(startEventResponse).isNotNull();
        }

        @Test
        void checkSubmitGaUpdateSubmitsTheUpdateWithGivenParametersWhenInvoked() {
            CaseData caseData = new CaseDataBuilder()
                .generalApplications(getGeneralApplications())
                .businessProcess(BusinessProcess.builder().status(STARTED).processInstanceId("1").build())
                .build();

            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);

            AboutToStartOrSubmitCallbackResponse response
                = (AboutToStartOrSubmitCallbackResponse) generalApplicationCreationHandler
                .handle(params);

            assertThat(caseData).isNotNull();
            verify(coreCaseDataService).triggerEvent(1L, CREATE_GENERAL_APPLICATION_CASE, Map.of());
        }

        private List<Element<GeneralApplication>> getGeneralApplications() {
            GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();

            builder.generalAppType(GAApplicationType.builder()
                .types(singletonList(EXTEND_TIME))
                .build());

            GeneralApplication application = builder

                .generalAppInformOtherParty(GAInformOtherParty.builder()
                    .isWithNotice(YES)
                    .reasonsForWithoutNotice(STRING_CONSTANT)
                    .build())
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder()
                    .generalAppUrgency(YES)
                    .reasonsForUrgency(STRING_CONSTANT)
                    .urgentAppConsiderationDate(APP_DATE_EPOCH)
                    .build())
                .isMultiParty(YesOrNo.NO)
                .businessProcess(BusinessProcess.builder()
                    .status(BusinessProcessStatus.READY)
                    .build())
                .build();
            return wrapElements(application);
        }
    }
}

