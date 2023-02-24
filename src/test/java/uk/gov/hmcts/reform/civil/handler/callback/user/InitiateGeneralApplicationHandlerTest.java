package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static java.lang.String.format;
import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder.CUSTOMER_REFERENCE;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@SpringBootTest(classes = {
    InitiateGeneralApplicationHandler.class,
    JacksonAutoConfiguration.class,
},
    properties = {"reference.database.enabled=false"})
class InitiateGeneralApplicationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private InitiateGeneralApplicationHandler handler;

    @Value("${civil.response-pack-url}")
    private static final String STRING_CONSTANT = "this is a string";
    private static final LocalDate APP_DATE_EPOCH = EPOCH;
    private static final String CONFIRMATION_BODY = "<br/> <p> Your application fee of £%s"
        + " is now due for payment. Your application will not be reviewed by the"
        + " court until this fee has been paid."
        + "%n%n <a href=\"%s\" target=\"_blank\">Pay your application fee </a> %n";

    private static final Fee FEE275 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(27500)).code("FEE0444").version("1").build();

    private CaseData getEmptyTestCase(CaseData caseData) {
        return caseData.toBuilder()
            .build();
    }

    private CaseData getReadyTestCaseData(CaseData caseData, boolean multipleGenAppTypes) {
        GAInformOtherParty withOrWithoutNotice = GAInformOtherParty.builder()
                .isWithNotice(YES)
                .reasonsForWithoutNotice(STRING_CONSTANT)
                .build();
        GARespondentOrderAgreement withOrWithoutConsent = GARespondentOrderAgreement.builder()
                .hasAgreed(NO).build();

        return getReadyTestCaseData(caseData, multipleGenAppTypes, withOrWithoutConsent, withOrWithoutNotice);
    }

    private CaseData getReadyTestCaseData(CaseData caseData,
                                          boolean multipleGenAppTypes,
                                          GARespondentOrderAgreement hasAgreed,
                                          GAInformOtherParty withOrWithoutNotice) {
        GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();
        if (multipleGenAppTypes) {
            builder.generalAppType(GAApplicationType.builder()
                    .types(Arrays.asList(EXTEND_TIME, SUMMARY_JUDGEMENT))
                    .build());
        } else {
            builder.generalAppType(GAApplicationType.builder()
                    .types(singletonList(EXTEND_TIME))
                    .build());
        }
        GeneralApplication application = builder

                .generalAppInformOtherParty(withOrWithoutNotice)
                .generalAppRespondentAgreement(hasAgreed)
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(FEE275)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder()
                        .generalAppUrgency(YES)
                        .reasonsForUrgency(STRING_CONSTANT)
                        .urgentAppConsiderationDate(APP_DATE_EPOCH)
                        .build())
                .isMultiParty(NO)
                .businessProcess(BusinessProcess.builder()
                        .status(BusinessProcessStatus.READY)
                        .build())
                .build();
        return getEmptyTestCase(caseData)
                .toBuilder()
                .generalApplications(wrapElements(application))
                .build();
    }

    @Nested
    class SubmittedCallback {

        @Test
        void handleEventsReturnsTheExpectedCallbackEvent() {
            assertThat(handler.handledEvents()).contains(INITIATE_GENERAL_APPLICATION);
        }

        @Test
        void shouldReturnExpectedSubmittedCallbackResponse_whenRespondentsDoesNotHaveRepresentation() {
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().ccdCaseReference(CASE_ID).build(), true);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
            GeneralApplication genapp = caseData.getGeneralApplications().get(0).getValue();

            String body = format(
                CONFIRMATION_BODY,
                genapp.getGeneralAppPBADetails().getFee().toPounds(),
                format("/cases/case-details/%s#Applications", CASE_ID));

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response).usingRecursiveComparison().isEqualTo(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader(
                        "# You have made an application")
                    .confirmationBody(body)
                    .build());
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(body);
        }

        @Test
        void shouldNotReturnBuildConfirmationIfGeneralApplicationIsEmpty() {
            CaseData caseData = getEmptyTestCase(CaseDataBuilder.builder().build());
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isNull();
        }
    }
}

