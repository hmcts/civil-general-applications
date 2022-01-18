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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.unwrapElements;
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
    private CaseData getTestCaseData(CaseData caseData) {
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
        void sholudReturnFirstReadyGeneralApplicationElement() {
            CaseData caseData = getTestCaseData(CaseDataBuilder.builder().build());
            List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
            Optional<Element<GeneralApplication>> generalApplicationElementOptional = generalApplications.stream()
                .filter(app -> app.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.READY
                && app.getValue().getBusinessProcess().getProcessInstanceId() == null).findFirst();
            if (generalApplicationElementOptional.isPresent()) {
                GeneralApplication generalApplicationElement = generalApplicationElementOptional.get().getValue();
                assertThat(generalApplicationElement).isNotNull();
            }
        }

        @Test
        void shouldReturnExpectedSubmittedCallbackResponse() {
            CaseData caseData = getTestCaseData(CaseDataBuilder.builder().build());

            GeneralApplication application = unwrapElements(caseData.getGeneralApplications()).get(0);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            List<GeneralApplicationTypes> types = application.getGeneralAppType().getTypes();
            String collect = types.stream().map(appType -> "<li>" + appType + "</li>")
                .collect(Collectors.joining());
            boolean isApplicationUrgent = Optional.of(application.getGeneralAppUrgencyRequirement()
                                                          .getGeneralAppUrgency() == YesOrNo.YES).orElse(true);
            boolean isMultiParty = Optional.of(application.getIsMultiParty() == YesOrNo.YES).orElse(true);
            boolean isNotified = Optional.of(application.getGeneralAppInformOtherParty().getIsWithNotice()
                                                 == YesOrNo.YES).orElse(true);
            String lastLine = format(PARTY_NOTIFIED, isMultiParty ? "parties'" : "party's",
                                     isNotified ? "has been notified" : "has not been notified"
            );
            String body = format(CONFIRMATION_SUMMARY,
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

