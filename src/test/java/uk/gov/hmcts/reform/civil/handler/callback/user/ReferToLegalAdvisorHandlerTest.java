package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.REFER_TO_LEGAL_ADVISOR;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.model.common.DynamicList.fromList;

@SpringBootTest(classes = {
    CaseDetailsConverter.class,
    ReferToLegalAdvisorHandler.class,
    JacksonAutoConfiguration.class,
    },
    properties = {"reference.database.enabled=false"})
public class ReferToLegalAdvisorHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    ReferToLegalAdvisorHandler handler;

    @Autowired
    CaseDetailsConverter caseDetailsConverter;

    private static final String CONFIRMATION_MESSAGE = "Test refer to Legal Advisor";

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(REFER_TO_LEGAL_ADVISOR);
    }

    @Test
    void buildResponseConfirmationReturnsCorrectMessage() {
        CallbackParams params = callbackParamsOf(
            getCase(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION),
            CallbackType.SUBMITTED
        );
        var response = (SubmittedCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getConfirmationBody()).isEqualTo(CONFIRMATION_MESSAGE);
    }

    @Test
    void aboutToStartCallbackChecksApplicationStateBeforeProceeding() {
        CallbackParams params = callbackParamsOf(
            getCase(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION),
            CallbackType.ABOUT_TO_START
        );
        List<String> errors = new ArrayList<>();
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    private CaseData getCase(CaseState state) {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        DynamicList dynamicListTest = fromList(getSampleCourLocations());
        Optional<DynamicListElement> first = dynamicListTest.getListItems().stream().findFirst();
        first.ifPresent(dynamicListTest::setValue);

        return CaseData.builder()
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                        .hearingPreferredLocation(dynamicListTest)
                                        .hearingPreferencesPreferredType(GAJudicialHearingType.IN_PERSON)
                                        .build())
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .hearingPreferredLocation(dynamicListTest)
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .build())
            .generalAppType(
                GAApplicationType
                    .builder()
                    .types(types).build())
            .businessProcess(BusinessProcess
                                 .builder()
                                 .camundaEvent("INITIATE_GENERAL_APPLICATION")
                                 .processInstanceId("11111")
                                 .status(BusinessProcessStatus.FINISHED)
                                 .activityId("anyActivity")
                                 .build())
            .ccdState(state)
            .build();
    }

    protected List<String> getSampleCourLocations() {
        return new ArrayList<>(Arrays.asList("ABCD - RG0 0AL", "PQRS - GU0 0EE", "WXYZ - EW0 0HE", "LMNO - NE0 0BH"));
    }
}
