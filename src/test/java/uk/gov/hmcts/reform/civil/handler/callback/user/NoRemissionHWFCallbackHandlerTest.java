package uk.gov.hmcts.reform.civil.handler.callback.user;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_APPLICANT_LIP_HWF;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@SpringBootTest(classes = {
    NoRemissionHWFCallbackHandler.class,
    JacksonAutoConfiguration.class})
 class NoRemissionHWFCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    NoRemissionHWFCallbackHandler handler;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    CaseDetailsConverter caseDetailsConverter;

    @MockBean
    HwFFeeTypeService hwFFeeTypeService;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(NO_REMISSION_HWF_GA);
    }

    @Test
    void shouldSubmit_NoRemissionHwFEventAndStartNotifyApplicantLip() {
        CaseData caseData = CaseData.builder()
            .ccdState(AWAITING_RESPONDENT_RESPONSE)
            .hwfFeeType(FeeType.APPLICATION)
            .generalAppPBADetails(GAPbaDetails.builder().fee(
                    Fee.builder()
                        .calculatedAmountInPence(BigDecimal.valueOf(500)).code("FEE205").build())
                                      .build())
            .gaHwfDetails(HelpWithFeesDetails.builder()
                               .remissionAmount(BigDecimal.valueOf(500))
                               .build())
            .generalAppHelpWithFees(HelpWithFees.builder().build()).build();
        CallbackParams params = callbackParamsOf(caseData, CaseEvent.NO_REMISSION_HWF_GA, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        //Then
        CaseData updatedData = objectMapper.convertValue(response.getData(), CaseData.class);

        assertThat(updatedData).isNotNull();
        assertThat(updatedData.getGaHwfDetails().getHwfCaseEvent()).isEqualTo(NO_REMISSION_HWF_GA);
        assertThat(updatedData.getBusinessProcess().getCamundaEvent()).isEqualTo(NOTIFY_APPLICANT_LIP_HWF.toString());
    }

}
