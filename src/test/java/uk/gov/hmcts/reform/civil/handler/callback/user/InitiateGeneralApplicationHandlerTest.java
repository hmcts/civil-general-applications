package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.*;
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
    private static final String CONFIRMATION_SUMMARY = "<br/><p> Your Court will make a decision on %s."
        + "<ul> %s </ul>"
        + "</p> %s"
        + " %s ";
    private static final String URGENT_APPLICATION = "<p> You have marked this application as urgent. </p>";
    private static final String PARTY_NOTIFIED = "<p> The other %s legal representative %s "
        + "that you have submitted this application.";

    private CaseData getEmptyTestCase(CaseData caseData) {
        return caseData.toBuilder()
            .build();
    }

    private CaseData getReadyTestCaseData(CaseData caseData, boolean multipleGenAppTypes) {
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

            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(NO)
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
        return getEmptyTestCase(caseData)
            .toBuilder()
            .generalApplications(wrapElements(application))
            .build();
    }

    private CaseData getStartedTestCaseData(CaseData caseData) {
        GeneralApplication application = GeneralApplication.builder()
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(NO)
                                            .reasonsForWithoutNotice(STRING_CONSTANT)
                                            .build())
            .generalAppUrgencyRequirement(GAUrgencyRequirement.builder()
                                              .generalAppUrgency(YES)
                                              .reasonsForUrgency(STRING_CONSTANT)
                                              .urgentAppConsiderationDate(APP_DATE_EPOCH)
                                              .build())
            .isMultiParty(YesOrNo.NO)
            .businessProcess(BusinessProcess.builder()
                                 .status(BusinessProcessStatus.STARTED)
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
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><p> Your Court will make a decision on this application.<ul> <li>EXTEND_TIME</li> </ul></p> <p> You have marked this application as urgent. </p> <p> The other party's legal representative has not been notified that you have submitted this application. ");
        }

        @Test
        void shouldReturnBuildConfirmationForMultipleApplicationType() {
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(), true);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><p> Your Court will make a decision on these applications.<ul> <li>EXTEND_TIME</li><li>SUMMARY_JUDGEMENT</li> </ul></p> <p> You have marked this application as urgent. </p> <p> The other party's legal representative has not been notified that you have submitted this application. ");
        }

        @Test
        void sholudNotReturnFirstReadyGeneralApplicationElement() {
            CaseData caseData = getStartedTestCaseData(CaseDataBuilder.builder().build());
            List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
            Optional<Element<GeneralApplication>> generalApplicationElementOptional = generalApplications.stream()
                .filter(app -> app.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.READY
                    && app.getValue().getBusinessProcess().getProcessInstanceId() == null).findFirst();
            if (generalApplicationElementOptional.isPresent()) {
                GeneralApplication generalApplicationElement = generalApplicationElementOptional.get().getValue();
                assertThat(generalApplicationElement).isNull();
            }
        }

        @Test
        void shouldReturnExpectedSubmittedCallbackResponse() {
            CaseData caseData = getReadyTestCaseData(CaseDataBuilder.builder().build(), false);
            List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
            Optional<Element<GeneralApplication>> generalApplicationElementOptional = generalApplications.stream()
                .filter(app -> app.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.READY
                    && app.getValue().getBusinessProcess().getProcessInstanceId() == null).findFirst();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            if (generalApplicationElementOptional.isPresent()) {
                GeneralApplication generalApplicationElement = generalApplicationElementOptional.get().getValue();
                List<GeneralApplicationTypes> types = generalApplicationElement.getGeneralAppType().getTypes();
                String collect = types.stream().map(appType -> "<li>" + appType + "</li>")
                    .collect(Collectors.joining());
                boolean isApplicationUrgent = Optional.of(generalApplicationElement.getGeneralAppUrgencyRequirement()
                                                              .getGeneralAppUrgency() == YesOrNo.YES).orElse(true);
                boolean isMultiParty = Optional.of(generalApplicationElement.getIsMultiParty()
                                                       == YesOrNo.YES).orElse(true);
                boolean isNotified = Optional.of(generalApplicationElement.getGeneralAppInformOtherParty()
                                                     .getIsWithNotice()
                                                     == YesOrNo.YES).orElse(true);
                String lastLine = format(PARTY_NOTIFIED, isMultiParty ? "parties'" : "party's",
                                         isNotified ? "has been notified" : "has not been notified"
                );
                String body = format(
                    CONFIRMATION_SUMMARY,
                    types.size() == 1 ? "this application" : "these applications",
                    collect,
                    isApplicationUrgent ? URGENT_APPLICATION : " ",
                    lastLine
                );
                assertThat(response).usingRecursiveComparison().isEqualTo(
                    SubmittedCallbackResponse.builder()
                        .confirmationHeader("# You have made an application")
                        .confirmationBody(body)
                        .build());
            }
        }
    }
}

