package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_GA_CASE_DATA;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.FINISHED;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@SpringBootTest(classes = {
    GeneralApplicationCreationCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
},
    properties = {"reference.database.enabled=false"})
class GeneralApplicationCreationCallbackHandlerTest extends BaseCallbackHandlerTest {

    private static final String STRING_CONSTANT = "this is a string";
    private static final LocalDate APP_DATE_EPOCH = EPOCH;
    private static final String PROCESS_INSTANCE_ID = "1";

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private GeneralApplicationCreationCallbackHandler generalApplicationCreationHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class AboutToSubmitCallBack {

        @Test
        void checkSubmitGaUpdateSubmitsTheUpdateWithGivenParametersWhenInvoked() {

            GeneralApplication generalApplication = getGeneralApplication();

            CaseData caseData = new CaseDataBuilder()
                .generalApplications(getGeneralApplications(generalApplication))
                .businessProcess(BusinessProcess.builder().status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID).build()).build();

            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);

            generalApplicationCreationHandler.createGeneralApplication(params);

            GeneralApplication expectedGeneralApplication = getGeneralApplication();

            Mockito.verify(coreCaseDataService).createGeneralAppCase(
                expectedGeneralApplication.toMap(objectMapper));

            expectedGeneralApplication.getBusinessProcess().setStatus(FINISHED);
            Map<String, Object> output = params.getRequest().getCaseDetails().getData();
            output.put("generalApplications", getGeneralApplications(expectedGeneralApplication));

            Mockito.verify(coreCaseDataService).triggerEvent(
                caseData.getCcdCaseReference(),
                UPDATE_GA_CASE_DATA,
                output);
        }

        @Test
        void shouldNotCallTriggerGaEventWhenNoGeneralApplicationDataExist() {
            CaseData caseData = new CaseDataBuilder()
                .generalApplications(null)
                .businessProcess(BusinessProcess.builder().status(STARTED)
                    .processInstanceId(PROCESS_INSTANCE_ID).build()).build();

            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);

            generalApplicationCreationHandler.createGeneralApplication(params);

            Mockito.verifyNoInteractions(coreCaseDataService);
        }

        private List<Element<GeneralApplication>> getGeneralApplications(GeneralApplication generalApplication) {
            return wrapElements(generalApplication);
        }

        private GeneralApplication getGeneralApplication() {
            GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();

            builder.generalAppType(GAApplicationType.builder()
                .types(singletonList(SUMMARY_JUDGEMENT))
                .build());

            return builder
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
                    .status(STARTED)
                    .processInstanceId(PROCESS_INSTANCE_ID)
                    .build())
                .build();
        }
    }
}

