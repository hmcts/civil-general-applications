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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.time.LocalDate;
import java.util.Arrays;

import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
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
    private static final String CONF_SINGLE_NOTIFIED = "<br/><p> Your Court will make a decision on this application."
        + "<ul> <li>Extend time</li> </ul>"
        + "</p> <p> You have marked this application as urgent. </p> <p> The other party's legal representative "
        + "has been notified that you have submitted this application. ";
    private static final String CONFIRMATION = "<br/><p> Your Court will make a decision on these applications."
        + "<ul> <li>Extend time</li><li>Summary judgment</li> </ul>"
        + "</p> <p> You have marked this application as urgent. </p> <p> The other party's legal representative "
        + "has been notified that you have submitted this application. ";

    private static final String CONF_SINGLE_NOT_NOTIFIED = "<br/><p> Your Court will make a decision on this "
            + "application.<ul> <li>Extend time</li> </ul></p> <p> You have marked this application as urgent. </p> "
            + "<p> The other party's legal representative has not been notified that you have submitted this "
            + "application. ";


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
        void shouldReturnBuildConfirmationForSingleApplicationType() {
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(), false);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOTIFIED);
        }

        @Test
        void shouldReturnBuildConfirmationForMultipleApplicationType() {
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(), true);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONFIRMATION);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementIsNull() {
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, null, null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOT_NOTIFIED);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementHasAgreedIsNull() {
            GARespondentOrderAgreement agreement = GARespondentOrderAgreement.builder().hasAgreed(null).build();
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, agreement, null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOT_NOTIFIED);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementHasAgreedIsYES() {
            GARespondentOrderAgreement agreement = GARespondentOrderAgreement.builder().hasAgreed(YES).build();
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, agreement, null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOT_NOTIFIED);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementHasAgreedIsNOAndInformPartyIsNull() {
            GARespondentOrderAgreement agreement = GARespondentOrderAgreement.builder().hasAgreed(NO).build();
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, agreement, null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOT_NOTIFIED);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementHasAgreedIsNOAndInformPartyNoticeIsNull() {
            GARespondentOrderAgreement agreement = GARespondentOrderAgreement.builder().hasAgreed(NO).build();
            GAInformOtherParty informOtherParty = GAInformOtherParty.builder().isWithNotice(null).build();
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, agreement, informOtherParty);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOT_NOTIFIED);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementHasAgreedIsNOAndInformPartyNoticeIsNO() {
            GARespondentOrderAgreement agreement = GARespondentOrderAgreement.builder().hasAgreed(NO).build();
            GAInformOtherParty informOtherParty = GAInformOtherParty.builder().isWithNotice(NO).build();
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, agreement, informOtherParty);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOT_NOTIFIED);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementHasAgreedIsYesAndInformPartyIsYes() {
            GARespondentOrderAgreement agreement = GARespondentOrderAgreement.builder().hasAgreed(YES).build();
            GAInformOtherParty informOtherParty = GAInformOtherParty.builder().isWithNotice(YES).build();
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, agreement, informOtherParty);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOT_NOTIFIED);
        }

        @Test
        void shouldSetIsNotifiedToFalseWhenAgreementHasAgreedIsNOAndInformPartyIsYes() {
            GARespondentOrderAgreement agreement = GARespondentOrderAgreement.builder().hasAgreed(NO).build();
            GAInformOtherParty informOtherParty = GAInformOtherParty.builder().isWithNotice(YES).build();
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(),
                    false, agreement, informOtherParty);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CONF_SINGLE_NOTIFIED);
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

