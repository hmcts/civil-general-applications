package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;

import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.service.docmosis.consentorder.ConsentOrderGenerator;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPROVE_CONSENT_ORDER;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    ApproveConsentOrderCallbackHandler.class,
    JacksonAutoConfiguration.class,
    ValidationAutoConfiguration.class,
    CaseDetailsConverter.class,
})
public class ApproveConsentOrderCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String CAMUNDA_EVENT = "APPROVE_CONSENT_ORDER";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    @Autowired
    private ApproveConsentOrderCallbackHandler handler;

    @MockBean
    private ConsentOrderGenerator consentOrderGenerator;

    @Test
    void handleEventsReturnsTheExpectedCallbackEventApproveConsentOrder() {
        assertThat(handler.handledEvents()).contains(APPROVE_CONSENT_ORDER);
    }

    @Nested
    class AboutToStartCallbackHandling {

        @Test
        void shouldReturnApproveConsentOrderEndDateEnableWhenApplicationTypeContainsStayTheClaim() {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            CallbackParams params = callbackParamsOf(getGeneralAppCaseData(types), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            CaseData data = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(data.getGeneralAppDetailsOfOrder()).isEqualTo(data.getApproveConsentOrder().getConsentOrderDescription());
            assertThat(data.getApproveConsentOrder().getShowConsentOrderDate()).isEqualTo(YES);
        }

        @Test
        void shouldNotReturnApproveConsentOrderEndDateWhenApplicationTypeDoesNotContainsStayTheClaim() {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            CallbackParams params = callbackParamsOf(getGeneralAppCaseData(types), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            CaseData data = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(data.getGeneralAppDetailsOfOrder()).isEqualTo(data.getApproveConsentOrder().getConsentOrderDescription());
            assertThat(data.getApproveConsentOrder().getShowConsentOrderDate()).isNull();
        }
    }

    @Nested
    class MidEventToValidate {

        private static final String VALIDATE_CONSENT_ORDER = "populate-consent-order-doc";

        @Test
        void shouldGenerateConsentOrderDocument() {
            List<GeneralApplicationTypes> types = List.of(
                                                          (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            CallbackParams params = callbackParamsOf(getGeneralAppCaseData(types), MID, "populate-consent-order-doc");
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(response).isNotNull();
        }
    }

    @Nested
    class SubmittedCallbackHandling {

        @Test
        void callbackHandlingForMakeAnOrder() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            CallbackParams params = callbackParamsOf(getGeneralAppCaseData(types), SUBMITTED);
            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response.getConfirmationHeader()).isEqualTo("# Your order has been made");
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><br/>");
        }
    }

    public CaseData getGeneralAppCaseData(List<GeneralApplicationTypes> types) {

        return CaseData.builder()
            .generalAppDetailsOfOrder("Testing prepopulated text")
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YES).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
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
                                 .status(BusinessProcessStatus.STARTED)
                                 .activityId(ACTIVITY_ID)
                                 .build())
            .ccdState(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .build();
    }
}
