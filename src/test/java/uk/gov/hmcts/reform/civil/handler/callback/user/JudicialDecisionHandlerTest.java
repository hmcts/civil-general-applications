package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.MakeAppAvailableCheckGAspec;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingSupportRequirements;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.enums.dq.SupportRequirements;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GAMakeApplicationAvailableCheck;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DeadlinesCalculator;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionHelper;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionWrittenRepService;
import uk.gov.hmcts.reform.civil.service.Time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.JUDGE_MAKES_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.formatLocalDate;
import static uk.gov.hmcts.reform.civil.service.JudicialDecisionWrittenRepService.WRITTEN_REPRESENTATION_DATE_CANNOT_BE_IN_PAST;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    JudicialDecisionHandler.class,
    DeadlinesCalculator.class,
    JacksonAutoConfiguration.class},
    properties = {"reference.database.enabled=false"})
public class JudicialDecisionHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    JudicialDecisionHandler handler;

    @MockBean
    JudicialDecisionWrittenRepService service;

    @MockBean
    JudicialDecisionHelper helper;

    @MockBean
    GeneralAppLocationRefDataService locationRefDataService;

    @MockBean
    private Time time;

    @MockBean
    private DeadlinesCalculator deadlinesCalculator;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CAMUNDA_EVENT = "INITIATE_GENERAL_APPLICATION";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yy");
    private static final DateTimeFormatter DATE_FORMATTER_SUBMIT_CALLBACK = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String expectedDismissalOrder = "This application is dismissed.\n\n"
            + "[Insert Draft Order from application]\n\n"
            + "A person who was not notified of the application before this order was made may apply to have the "
            + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
            + "notification of the order.";
    private static final String PERSON_NOT_NOTIFIED_TEXT = "\n\n"
            + "A person who was not notified of the application"
            + " before the order was made may apply to have the order set aside or varied."
            + " Any application under this paragraph must be made within 7 days.";

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(JUDGE_MAKES_DECISION);
    }

    @Nested
    class AboutToStartCallbackHandling {
        YesOrNo hasRespondentResponseVul = NO;

        @Test
        void testAboutToStartForHearingGeneralOrderRecital() {

            String expecetedJudicialTimeEstimateText = "Both applicant and respondent estimate it would take %s.";
            String expecetedJudicialPreferrenceText = "Both applicant and respondent prefer %s.";
            when(helper.isApplicantAndRespondentLocationPrefSame(any())).thenReturn(true);
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, YES), ABOUT_TO_START);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingTimeEstimateText1())
                .isEqualTo(String.format(expecetedJudicialTimeEstimateText, getHearingOrderApplnAndResp(types, NO, YES)
                    .getGeneralAppHearingDetails().getHearingDuration().getDisplayedValue()));

            assertThat(responseCaseData.getHearingPreferencesPreferredTypeLabel1())
                .isEqualTo(String.format(expecetedJudicialPreferrenceText, getHearingOrderApplnAndResp(types, NO, YES)
                    .getGeneralAppHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue()));

        }

        @Test
        void testAboutToStartForHearingPreferLocationsApplicantRespondent() {

            String expectedJudicialPreferenceLocationApplicantRespondent1Text =
                "Applicant prefers Location %s. Respondent1 prefers Location %s.";

            when(helper.isApplicantAndRespondentLocationPrefSame(any())).thenReturn(false);

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            var caseDataApplicantRespondent1 = getHearingOrderAppForCourtLocationPreference(types, YES, YES,
                                                                                            NO);
            var caseDataApplicantRespondent2 = getHearingOrderAppForCourtLocationPreference(types, YES, NO,
                                                                                            YES);

            CallbackParams params = callbackParamsOf(caseDataApplicantRespondent1, ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);
            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(String.format(expectedJudicialPreferenceLocationApplicantRespondent1Text,
                                         caseDataApplicantRespondent1.getGeneralAppHearingDetails()
                                             .getHearingPreferredLocation().getValue().getLabel(),
                                         caseDataApplicantRespondent1.getRespondentsResponses().get(0).getValue()
                                             .getGaHearingDetails().getHearingPreferredLocation().getValue()
                                             .getLabel()));

            params = callbackParamsOf(caseDataApplicantRespondent2, ABOUT_TO_START);
            response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            responseCaseData = getJudicialHearingOrder(response);
            String expectedJudicialPreferenceLocationApplicantRespondent2Text =
                "Applicant prefers Location %s. Respondent2 prefers Location %s.";

            assertThat(response).isNotNull();
            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(String.format(expectedJudicialPreferenceLocationApplicantRespondent2Text,
                                         caseDataApplicantRespondent2.getGeneralAppHearingDetails()
                                             .getHearingPreferredLocation().getValue().getLabel(),
                                         caseDataApplicantRespondent2.getRespondentsResponses().get(1).getValue()
                                             .getGaHearingDetails().getHearingPreferredLocation().getValue()
                                             .getLabel()));

        }

        @Test
        void testAboutToStartForHearingOnlyRespondent1Respondent2LocationPreference() {

            String expectedOnlyRespondent1LocationText =
                "Respondent1 prefers Location %s. Respondent2 prefers Location %s.";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            var caseData = getHearingOrderAppForCourtLocationPreference(types, NO, YES, YES);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(String.format(expectedOnlyRespondent1LocationText,
                                         caseData.getRespondentsResponses().get(0).getValue().getGaHearingDetails()
                                             .getHearingPreferredLocation().getValue().getLabel(),
                                         caseData.getRespondentsResponses().get(1).getValue().getGaHearingDetails()
                                            .getHearingPreferredLocation().getValue().getLabel()));
        }

        @Test
        void testAboutToStartForHearingOnlyRespondent1LocationPreference() {

            String expectedOnlyRespondent1LocationText = "Respondent1 prefers Location %s.";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            var caseData = getHearingOrderAppForCourtLocationPreference(types, NO, YES, NO);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(String.format(expectedOnlyRespondent1LocationText,
                                         caseData.getRespondentsResponses().get(0).getValue().getGaHearingDetails()
                                             .getHearingPreferredLocation().getValue().getLabel()));
        }

        @Test
        void testAboutToStartForHearingOnlyRespondent2LocationPreference() {

            String expectedOnlyRespondent2LocationText = "Respondent2 prefers Location %s.";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            var caseData = getHearingOrderAppForCourtLocationPreference(types, NO, NO, YES);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(String.format(expectedOnlyRespondent2LocationText,
                                         caseData.getRespondentsResponses().get(1).getValue().getGaHearingDetails()
                                             .getHearingPreferredLocation().getValue().getLabel()));
        }

        @Test
        void testAboutToStartForHearingOnlyApplicantLocationPreference() {

            String expectedOnlyRespondent1LocationText = "Applicant prefers Location %s.";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            var caseData = getHearingOrderAppForCourtLocationPreference(types, YES, NO, NO);
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(String.format(expectedOnlyRespondent1LocationText, caseData.getGeneralAppHearingDetails()
                    .getHearingPreferredLocation().getValue().getLabel()));
        }

        @Test
        void testAboutToStartForHearingDetails() {

            String expecetedJudicialTimeEstimateText =
                "Applicant estimates %s. Respondent1 estimates %s. Respondent2 estimates %s.";
            String expecetedJudicialPreferrenceText =
                "Applicant prefers %s. Respondent1 prefers %s. Respondent2 prefers %s.";
            String expecetedJudicialSupportText =
                "Applicant require %s. Respondent1 require %s. Respondent2 require %s.";
            String expectedJudicialPreferenceLocationText =
                "Applicant prefers Location %s. Respondent1 prefers Location %s. Respondent2 prefers Location %s.";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp1and2(types, NO, YES, YES),
                ABOUT_TO_START
            );

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingTimeEstimateText1())
                .isEqualTo(String.format(
                    expecetedJudicialTimeEstimateText,
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getGeneralAppHearingDetails().getHearingDuration()
                        .getDisplayedValue(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(0).getValue().getGaHearingDetails()
                        .getHearingDuration().getDisplayedValue(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(1).getValue().getGaHearingDetails()
                        .getHearingDuration().getDisplayedValue()
                ));

            assertThat(responseCaseData.getHearingPreferencesPreferredTypeLabel1())
                .isEqualTo(String.format(
                    expecetedJudicialPreferrenceText,
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getGeneralAppHearingDetails().getHearingPreferencesPreferredType()
                        .getDisplayedValue(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(0).getValue().getGaHearingDetails()
                        .getHearingPreferencesPreferredType().getDisplayedValue(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(1).getValue().getGaHearingDetails()
                        .getHearingPreferencesPreferredType().getDisplayedValue()
                ));
            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(String.format(
                    expectedJudicialPreferenceLocationText,
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getGeneralAppHearingDetails().getHearingPreferredLocation()
                        .getValue().getLabel(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(0).getValue().getGaHearingDetails()
                        .getHearingPreferredLocation().getValue().getLabel(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(1).getValue().getGaHearingDetails()
                        .getHearingPreferredLocation().getValue().getLabel()));

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
                .isEqualTo(String.format(
                    expecetedJudicialSupportText,
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getGeneralAppHearingDetails()
                        .getSupportRequirement().get(0).getDisplayedValue(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(0).getValue()
                        .getGaHearingDetails().getSupportRequirement()
                        .get(0).getDisplayedValue(),
                    getHearingOrderApplnAndResp1and2(types, NO, YES, YES)
                        .getRespondentsResponses().get(1).getValue()
                        .getGaHearingDetails().getSupportRequirement()
                        .get(0).getDisplayedValue()
                ));

        }

        @Test
        void shouldReturnEmptyStringForNullSupportReq() {

            List<Element<GARespondentResponse>> respondentResponse = null;

            GAUrgencyRequirement urgentApp = null;

            CallbackParams params = callbackParamsOf(getCaseDateWithNoSupportReq(
                respondentResponse,
                urgentApp
            ), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingTimeEstimateText1())
                .isEqualTo("Applicant estimates 1 hour");

            assertThat(responseCaseData.getHearingPreferencesPreferredTypeLabel1())
                .isEqualTo("Applicant prefers In person");

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
                .isEqualTo("Applicant require(s) no support");

            assertThat(responseCaseData.getJudgeHearingCourtLocationText1())
                .isEqualTo(StringUtils.EMPTY);

        }

        @Test
        void testAboutToStartForHearingScreenForUrgentApp() {

            String expecetedJudicialTimeEstimateText = "Applicant estimates 1 hour";
            String expecetedJudicialPreferrenceText = "Applicant prefers In person";
            String expecetedJudicialSupportReqText = "Applicant require(s) Hearing loop, Other support";

            CallbackParams params = callbackParamsOf(getCaseDateForUrgentApp(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingTimeEstimateText1())
                .isEqualTo(expecetedJudicialTimeEstimateText);

            assertThat(responseCaseData.getHearingPreferencesPreferredTypeLabel1())
                .isEqualTo(expecetedJudicialPreferrenceText);

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
                .isEqualTo(expecetedJudicialSupportReqText);

        }

        @Test
        void testHearingScreenSupportReqWithNoApplnHearingSupport() {

            String expecetedJudicialSupportReqText = "Applicant require(s) no support";

            GAUrgencyRequirement urgentApp = GAUrgencyRequirement.builder().generalAppUrgency(YES).build();

            CallbackParams params = callbackParamsOf(getCaseDateWithNoSupportReq(
                null,
                urgentApp
            ), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
                .isEqualTo(expecetedJudicialSupportReqText);

        }

        @Test
        void testHearingScreenSupportReqWithNoApplnHearingSupportAndRespWithSupportReq() {

            String expecetedJudicialSupportReqText = "Applicant require no support. "
                + "Respondent require Other support, Hearing loop.";

            GAUrgencyRequirement urgentApp = GAUrgencyRequirement.builder().generalAppUrgency(YES).build();

            List<Element<GARespondentResponse>> respondentResponse = getRespodentResponses(hasRespondentResponseVul);

            CallbackParams params = callbackParamsOf(getCaseDateWithNoApplicantSupportReq(
                respondentResponse,
                urgentApp
            ), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
                .isEqualTo(expecetedJudicialSupportReqText);

        }

        public String getJudgeHearingSupportReqText(CaseData caseData, YesOrNo isAppAndRespSameSupportReq) {

            String judicialSupportReqText1 = "Applicant require "
                + "%s. Respondent require %s.";
            String judicialSupportReqText2 = " Both applicant and respondent require %s.";

            List<String> applicantSupportReq
                = caseData.getGeneralAppHearingDetails().getSupportRequirement()
                .stream().map(GAHearingSupportRequirements::getDisplayedValue).collect(Collectors.toList());

            List<String> respondentSupportReq
                = caseData.getRespondentsResponses().stream().iterator().next().getValue()
                .getGaHearingDetails().getSupportRequirement().stream()
                .map(GAHearingSupportRequirements::getDisplayedValue)
                .collect(Collectors.toList());

            String appSupportReq = String.join(", ", applicantSupportReq);
            String resSupportReq = String.join(", ", respondentSupportReq);

            return isAppAndRespSameSupportReq == YES ? format(judicialSupportReqText2, appSupportReq)
                : format(judicialSupportReqText1, appSupportReq, resSupportReq);
        }

        @Test
        void shouldMatchHearingReqForDifferentPreferences() {

            String expecetedJudicialTimeEstimateText = "Applicant estimates 45 minutes. Respondent estimates 1 hour.";
            String expecetedJudicialPreferrenceText = "Applicant prefers Video conference hearing. Respondent "
                + "prefers In person.";
            String expecetedJudicialSupportReqText = "Applicant require Disabled access, Sign language interpreter. "
                + "Respondent require Other support, Hearing loop.";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getCaseDateWithHearingScreeen1V1(types, NO, YES), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingTimeEstimateText1())
                .isEqualTo(expecetedJudicialTimeEstimateText);

            assertThat(responseCaseData.getHearingPreferencesPreferredTypeLabel1())
                .isEqualTo(expecetedJudicialPreferrenceText);

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
                .isEqualTo(expecetedJudicialSupportReqText);

        }

        @Test
        void testAboutToStartForNotifiedApplication() {
            String expectedRecitalText = "<Title> <Name> \n"
                + "Upon reading the application of Claimant dated 15 January 22 and upon the "
                + "application of ApplicantPartyName dated %s and upon considering the information "
                + "provided by the parties";
            when(helper.isApplicationCloaked(any())).thenReturn(NO);
            CallbackParams params = callbackParamsOf(getNotifiedApplication(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(NO);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(
                expectedRecitalText,
                DATE_FORMATTER.format(LocalDate.now())
            ));
            assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
        }

        @Test
        void testAboutToStartForCloakedApplication() {
            String expectedRecitalText = "<Title> <Name> \n"
                + "Upon reading the application of Claimant dated 15 January 22 and upon the "
                + "application of ApplicantPartyName dated %s and upon considering the information "
                + "provided by the parties";
            when(helper.isApplicationCloaked(any())).thenReturn(YES);
            CallbackParams params = callbackParamsOf(getCloakedApplication(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(YES);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(
                expectedRecitalText,
                DATE_FORMATTER.format(LocalDate.now())
            ));
            assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
        }

        @Test
        void testAboutToStartForDefendant_judgeRecitalText() {
            String expectedRecitalText = "<Title> <Name> \n"
                + "Upon reading the application of Defendant dated 15 January 22 and upon the "
                + "application of ApplicantPartyName dated %s and upon considering the information "
                + "provided by the parties";
            when(helper.isApplicationCloaked(any())).thenReturn(NO);
            CallbackParams params = callbackParamsOf(getApplicationByParentCaseDefendant(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(NO);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(
                expectedRecitalText,
                DATE_FORMATTER.format(LocalDate.now())
            ));
            assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
        }

        @Test
        void testAboutToStartForDefendant_orderText() {
            when(helper.isApplicationCloaked(any())).thenReturn(NO);
            CallbackParams params = callbackParamsOf(getApplicationByParentCaseDefendant(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(NO);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getOrderText())
                .isEqualTo("Draft order text entered by applicant." + PERSON_NOT_NOTIFIED_TEXT);

        }

        @Test
        void shouldReturnYesForJudgeApproveEditOptionDateIfGATypeIsStayClaim() {

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STAY_THE_CLAIM));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDate())
                .isEqualTo(YES);

        }

        @Test
        void shouldReturnYesForJudgeApproveEditOptionDateIfGATypeIsStayClaimAndExtendTime() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM),
                (GeneralApplicationTypes.EXTEND_TIME)
            );

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDate())
                .isEqualTo(YES);

        }

        @Test
        void shouldReturnNOForJudgeApproveEditOptionDateIfGATypesIsNotEitherExtendTimeORStayClaim() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDate())
                .isEqualTo(NO);

        }

        @Test
        void shouldReturnYESForjudgeApproveEditOptionDateIfGATypesIsNotEitherExtendTimeORStayClaim() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STAY_THE_CLAIM), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDate())
                .isEqualTo(YES);

        }

        @Test
        void shouldReturnYesForJudgeApproveEditOptionPartyIfGATypeIsExtendTime() {

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.EXTEND_TIME));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(YES);

        }

        @Test
        void shouldReturnYesForJudgeApproveEditOptionPartyIfGATypeIsStrikeOut() {

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STRIKE_OUT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(YES);

        }

        @Test
        void shouldReturnYesForJudgeApproveEditOptionPartyIfGATypeIsStayClaimAndExtendTime() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.STRIKE_OUT),
                (GeneralApplicationTypes.EXTEND_TIME)
            );

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(YES);

        }

        @Test
        void shouldReturnCorrectDirectionOrderText_whenJudgeMakeDecisionGiveDirection() {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getDirectionOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDirectionsText()).isEqualTo(PERSON_NOT_NOTIFIED_TEXT);
        }

        @Test
        void shouldReturnNOForJudgeApproveEditOptionPartyIfGATypesIsNotEitherExtendTimeORStayClaim() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(NO);

        }

        @Test
        void shouldReturnNOForjudgeApproveEditOptionDocIfGATypesIsNotEitherExtendTimeORStrikeOut() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(YES);

        }

        @Test
        void shouldMatchExpectedVulnerabilityText() {

            String expecetedVulnerabilityText = "Applicant requires support with regards to vulnerability\n"
                + "dummy\n\nRespondent requires support with regards to vulnerability\ndummy";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, YES, YES), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithRespondentRespond() {

            String expecetedVulnerabilityText = "Respondent requires support with regards to vulnerability\n"
                + "dummy";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp(types, NO, YES),
                ABOUT_TO_START
            );
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithRespondent1and2Respond() {

            String expecetedVulnerabilityText = "\n\nRespondent1 requires support with regards to vulnerability\n"
                + "dummy1\n\nRespondent2 requires support with regards to vulnerability\ndummy2";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp1and2(types, NO, YES, YES),
                ABOUT_TO_START
            );
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithApplicantRespondent1and2Respond() {

            String expecetedVulnerabilityText = "Applicant requires support with regards to vulnerability\ndummy"
                + "\n\nRespondent1 requires support with regards to vulnerability\n"
                + "dummy1\n\nRespondent2 requires support with regards to vulnerability\ndummy2";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp1and2(types, YES, YES, YES),
                ABOUT_TO_START
            );
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithApplicantRespondent1Respond() {

            String expecetedVulnerabilityText = "Applicant requires support with regards to vulnerability\ndummy"
                + "\n\nRespondent1 requires support with regards to vulnerability\n"
                + "dummy1";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp1and2(types, YES, YES, NO),
                ABOUT_TO_START
            );
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithApplicantRespondent2Respond() {

            String expecetedVulnerabilityText = "Applicant requires support with regards to vulnerability\ndummy"
                + "\n\nRespondent2 requires support with regards to vulnerability\ndummy2";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp1and2(types, YES, NO, YES),
                ABOUT_TO_START
            );
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithRespondent2Respond() {

            String expecetedVulnerabilityText =
                "\n\nRespondent2 requires support with regards to vulnerability\ndummy2";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp1and2(types, NO, NO, YES),
                ABOUT_TO_START
            );
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithRespondent1Respond() {

            String expecetedVulnerabilityText =
                "\n\nRespondent1 requires support with regards to vulnerability\ndummy1";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(
                getHearingOrderApplnAndResp1and2(types, NO, YES, NO),
                ABOUT_TO_START
            );
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldHaveVulTextWithApplicantRespond() {

            String expecetedVulnerabilityText = "Applicant requires support with regards to vulnerability\n"
                + "dummy";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, YES, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldReturnExpectedTextWithNOVulRespond() {

            String expecetedVulnerabilityText = "No support required with regards to vulnerability";

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudicialVulnerabilityText())
                .isEqualTo(expecetedVulnerabilityText);

        }

        @Test
        void shouldPrepopulateLocationIfApplicantAndRespondentHaveSameLocationPref() {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            when(locationRefDataService.getCourtLocations(any()))
                .thenReturn(List.of("ABCD - RG0 0AL", "PQRS - GU0 0EE", "WXYZ - EW0 0HE", "LMNO - NE0 0BH"));
            when(helper.isApplicantAndRespondentLocationPrefSame(any())).thenReturn(true);
            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getHearingPreferredLocation()).isNotNull();
            assertThat(responseCaseData.getHearingPreferredLocation().getValue()).isNotNull();
            assertThat(responseCaseData.getHearingPreferredLocation().getValue().getLabel())
                .isEqualTo("ABCD - RG0 0AL");

        }

        private GAJudgesHearingListGAspec getJudicialHearingOrder(AboutToStartOrSubmitCallbackResponse response) {
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
            return responseCaseData.getJudicialListForHearing();
        }

        private CaseData getCaseDateForUrgentApp() {

            hasRespondentResponseVul = NO;

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppHearingDetails(GAHearingDetails.builder()
                                        .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                        .hearingDuration(GAHearingDuration.HOUR_1)
                                        .supportRequirement(getApplicantResponses())
                                        .build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondent1Representative(
                    GARespondentRepresentative.builder()
                        .generalAppRespondent1Representative(YES)
                        .build())
                .generalAppHearingDetails(GAHearingDetails.builder()
                                              .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                              .hearingDuration(GAHearingDuration.HOUR_1)
                                              .supportRequirement(getApplicantResponses())
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

        private CaseData getCaseDateWithNoSupportReq(List<Element<GARespondentResponse>> respondentResponse,
                                                     GAUrgencyRequirement urgentApp) {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            hasRespondentResponseVul = NO;

            return CaseData.builder()
                .generalAppUrgencyRequirement(urgentApp)
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .respondentsResponses(respondentResponse)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondent1Representative(
                    GARespondentRepresentative.builder()
                        .generalAppRespondent1Representative(YES)
                        .build())
                .generalAppHearingDetails(GAHearingDetails.builder()
                                              .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                              .hearingDuration(GAHearingDuration.HOUR_1)
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

        private CaseData getCaseDateWithNoApplicantSupportReq(List<Element<GARespondentResponse>> respondentResponse,
                                                     GAUrgencyRequirement urgentApp) {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            hasRespondentResponseVul = NO;

            return CaseData.builder()
                .generalAppUrgencyRequirement(urgentApp)
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .respondentsResponses(respondentResponse)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondent1Representative(
                    GARespondentRepresentative.builder()
                        .generalAppRespondent1Representative(YES)
                        .build())
                .generalAppHearingDetails(GAHearingDetails.builder()
                                              .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                              .hearingDuration(GAHearingDuration.HOUR_1)
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

        public CaseData getCaseDateWithHearingScreeen1V1(List<GeneralApplicationTypes> types, YesOrNo vulQuestion,
                                                         YesOrNo hasRespondentResponseVul) {

            return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondent1Representative(
                    GARespondentRepresentative.builder()
                        .generalAppRespondent1Representative(YES)
                        .build())
                .generalAppHearingDetails(GAHearingDetails.builder()
                                              .vulnerabilityQuestionsYesOrNo(vulQuestion)
                                              .vulnerabilityQuestion("dummy")
                                              .hearingPreferencesPreferredType(GAHearingType.VIDEO)
                                              .hearingDuration(GAHearingDuration.MINUTES_45)
                                              .supportRequirement(getApplicantResponses1V1())
                                              .hearingPreferredLocation(getLocationDynamicList())
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

        public List<GAHearingSupportRequirements> getApplicantResponses1V1() {
            List<GAHearingSupportRequirements> applSupportReq = new ArrayList<>();
            applSupportReq
                .add(GAHearingSupportRequirements.DISABLED_ACCESS);
            applSupportReq
                .add(GAHearingSupportRequirements.SIGN_INTERPRETER);

            return applSupportReq;
        }

        private CaseData getCloakedApplication() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            hasRespondentResponseVul = NO;

            return CaseData.builder()
                .parentClaimantIsApplicant(YES)
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
                .generalAppHearingDetails(GAHearingDetails.builder()
                                              .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                              .hearingDuration(GAHearingDuration.HOUR_1)
                                              .supportRequirement(getApplicantResponses())
                                              .build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().build())
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

        private CaseData getApplicationByParentCaseDefendant() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            hasRespondentResponseVul = NO;

            return CaseData.builder()
                .parentClaimantIsApplicant(NO)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().build())
                .generalAppDetailsOfOrder("Draft order text entered by applicant.")
                .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().isWithNotice(YES).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                .isWithNotice(YES).build())
                .generalAppHearingDetails(GAHearingDetails.builder()
                                              .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                              .hearingDuration(GAHearingDuration.HOUR_1)
                                              .supportRequirement(getApplicantResponses())
                                              .build())
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

    @Nested
    class MidEventForWrittenRepresentation {

        private static final String VALIDATE_WRITTEN_REPRESENTATION_PAGE = "ga-validate-written-representation-date";
        private static final String VALIDATE_HEARING_ORDER_SCREEN = "validate-hearing-order-screen";

        @Test
        void shouldReturnErrors_whenSequentialWrittenRepresentationDateIsInPast() {
            CallbackParams params = callbackParamsOf(
                    getSequentialWrittenRepresentationDecision(LocalDate.now().minusDays(1)),
                    MID,
                    VALIDATE_WRITTEN_REPRESENTATION_PAGE
            );
            when(service.validateWrittenRepresentationsDates(any())).thenCallRealMethod();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(WRITTEN_REPRESENTATION_DATE_CANNOT_BE_IN_PAST);
        }

        @Test
        void shouldReturnErrors_whenConcurrentWrittenRepresentationDateIsInPast() {
            CallbackParams params = callbackParamsOf(
                    getConcurrentWrittenRepresentationDecision(LocalDate.now().minusDays(1)),
                    MID,
                    VALIDATE_WRITTEN_REPRESENTATION_PAGE
            );
            when(service.validateWrittenRepresentationsDates(any())).thenCallRealMethod();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(WRITTEN_REPRESENTATION_DATE_CANNOT_BE_IN_PAST);
        }

        @Test
        void shouldNotReturnErrors_whenSequentialWrittenRepresentationDateIsInFuture() {

            String expectedSequentialText = "The respondent may upload any written representations by 4pm on %s";
            String expectedApplicantSequentialText =
                "The applicant may upload any written representations by 4pm on %s";

            CallbackParams params = callbackParamsOf(
                getSequentialWrittenRepresentationDecision(LocalDate.now()),
                MID,
                VALIDATE_WRITTEN_REPRESENTATION_PAGE
            );
            when(service.validateWrittenRepresentationsDates(any())).thenCallRealMethod();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response.getErrors()).isEmpty();
            assertThat(responseCaseData.getJudicialSequentialDateText())
                .isEqualTo(String.format(expectedSequentialText, formatLocalDate(
                    responseCaseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                        .getWrittenSequentailRepresentationsBy(), DATE)));
            assertThat(responseCaseData.getJudicialApplicanSequentialDateText())
                .isEqualTo(String.format(expectedApplicantSequentialText, formatLocalDate(
                    responseCaseData
                        .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                        .getSequentialApplicantMustRespondWithin(), DATE)));
        }

        @Test
        void shouldNotReturnErrors_whenConcurrentWrittenRepresentationDateIsInFuture() {

            String expectedConcurrentText =
                "The applicant and respondent must respond with written representations by 4pm on %s";

            CallbackParams params = callbackParamsOf(
                getConcurrentWrittenRepresentationDecision(LocalDate.now()),
                MID,
                VALIDATE_WRITTEN_REPRESENTATION_PAGE
            );
            when(service.validateWrittenRepresentationsDates(any())).thenCallRealMethod();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response.getErrors()).isEmpty();
            assertThat(responseCaseData.getJudicialConcurrentDateText())
                .isEqualTo(String.format(expectedConcurrentText, formatLocalDate(
                    responseCaseData
                        .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                        .getWrittenConcurrentRepresentationsBy(), DATE)));
        }

        @Test
        void shouldPopulateJudicialGOHearingAndTimeEst() {

            String expectedJudicialHearingTypeText = "Hearing type is %s";
            String expeceedJudicialTimeEstimateText = "Estimated length of hearing is %s";

            List<SupportRequirements> judgeSupportReqChoices = new ArrayList<>();

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(judgeSupportReqChoices),
                                                     MID, VALIDATE_HEARING_ORDER_SCREEN);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getJudicialHearingGeneralOrderHearingText())
                .isEqualTo(String.format(expectedJudicialHearingTypeText, responseCaseData
                                   .getJudicialListForHearing().getHearingPreferencesPreferredType()
                    .getDisplayedValue()));

            assertThat(responseCaseData.getJudicialGeneralOrderHearingEstimationTimeText())
                .isEqualTo(String.format(expeceedJudicialTimeEstimateText, responseCaseData
                    .getJudicialListForHearing().getJudicialTimeEstimate().getDisplayedValue()));
        }

        @Test
        void shouldMatchJudgeGOSupportReqWithExpectedTextWithSeperator() {

            List<SupportRequirements> judgeSupportReqChoices = new ArrayList<>();
            judgeSupportReqChoices
                .add(SupportRequirements.HEARING_LOOPS);
            judgeSupportReqChoices
                .add(SupportRequirements.OTHER_SUPPORT);

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(judgeSupportReqChoices),
                                                     MID, VALIDATE_HEARING_ORDER_SCREEN);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            String expecetedJudicialSupportReqText = "Hearing requirements Hearing loop, Other support";

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response).isNotNull();

            assertThat(responseCaseData.getJudicialHearingGOHearingReqText())
                .isEqualTo(expecetedJudicialSupportReqText);

        }

        @Test
        void shouldMatchJudgeGOSupportReqWithExpectedText() {

            List<SupportRequirements> judgeSupportReqChoices = new ArrayList<>();
            judgeSupportReqChoices
                .add(SupportRequirements.HEARING_LOOPS);

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(judgeSupportReqChoices),
                                                     MID, VALIDATE_HEARING_ORDER_SCREEN);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            String expecetedJudicialSupportReqText = "Hearing requirements Hearing loop";

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response).isNotNull();

            assertThat(responseCaseData.getJudicialHearingGOHearingReqText())
                .isEqualTo(expecetedJudicialSupportReqText);

        }

        @Test
        void shouldReturnErrorWhenInPersonAndLocationIsNull() {

            CallbackParams params = callbackParamsOf(getJudicialListHearingData(),
                                                     MID, VALIDATE_HEARING_ORDER_SCREEN);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            String expectedErrorText = "Select your preferred hearing location.";

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response).isNotNull();
            assertThat(response.getErrors().contains(expectedErrorText));
        }

        @Test
        void shouldReturnNullForJudgeGOSupportRequirement() {

            List<SupportRequirements> judgeSupportReqChoices = null;

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(judgeSupportReqChoices),
                                                     MID, VALIDATE_HEARING_ORDER_SCREEN);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response).isNotNull();

            assertThat(responseCaseData.getJudicialHearingGOHearingReqText()).isEmpty();

        }

        private CaseData getJudicialListHearingData() {

            YesOrNo hasRespondentResponseVul = NO;

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                            .hearingPreferencesPreferredType(GAJudicialHearingType.IN_PERSON)
                                            .judicialTimeEstimate(GAHearingDuration.HOURS_2).build())
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

        private CaseData getHearingOrderApplnAndResp(List<SupportRequirements> judgeSupportReqChoices) {

            YesOrNo hasRespondentResponseVul = NO;

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                            .hearingPreferredLocation(getLocationDynamicList())
                                            .judicialSupportRequirement(judgeSupportReqChoices)
                                            .hearingPreferencesPreferredType(GAJudicialHearingType.VIDEO)
                                            .judicialTimeEstimate(GAHearingDuration.HOURS_2).build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppHearingDetails(GAHearingDetails.builder()
                                        .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                        .hearingPreferredLocation(getLocationDynamicList())
                                        .hearingDuration(GAHearingDuration.HOUR_1)
                                        .supportRequirement(getApplicantResponses())
                                        .build())
                .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppHearingDetails(GAHearingDetails.builder()
                                              .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                              .hearingPreferredLocation(getLocationDynamicList())
                                              .hearingDuration(GAHearingDuration.HOUR_1)
                                              .supportRequirement(getApplicantResponses())
                                              .build())
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

        public CaseData getSequentialWrittenRepresentationDecision(LocalDate writtenRepresentationDate) {

            GAJudicialWrittenRepresentations.GAJudicialWrittenRepresentationsBuilder
                    writtenRepresentationBuilder = GAJudicialWrittenRepresentations.builder();
            writtenRepresentationBuilder.writtenOption(GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS)
                    .writtenSequentailRepresentationsBy(writtenRepresentationDate)
                    .sequentialApplicantMustRespondWithin(writtenRepresentationDate)
                    .writtenConcurrentRepresentationsBy(null);

            GAJudicialWrittenRepresentations gaJudicialWrittenRepresentations = writtenRepresentationBuilder.build();
            return CaseData.builder()
                    .judicialDecisionMakeAnOrderForWrittenRepresentations(gaJudicialWrittenRepresentations).build();
        }

        public CaseData getConcurrentWrittenRepresentationDecision(LocalDate writtenRepresentationDate) {
            GAJudicialWrittenRepresentations.GAJudicialWrittenRepresentationsBuilder
                    writtenRepresentationBuilder = GAJudicialWrittenRepresentations.builder();
            writtenRepresentationBuilder.writtenOption(GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS)
                    .writtenConcurrentRepresentationsBy(writtenRepresentationDate)
                    .writtenSequentailRepresentationsBy(null);

            GAJudicialWrittenRepresentations gaJudicialWrittenRepresentations = writtenRepresentationBuilder.build();
            return CaseData.builder()
                    .judicialDecisionMakeAnOrderForWrittenRepresentations(gaJudicialWrittenRepresentations).build();
        }

    }

    @Nested
    class MidEventForMakeAnOrderOption {

        private static final String VALIDATE_MAKE_AN_ORDER = "validate-make-an-order";

        @Test
        void shouldReturnNOForJudgeApproveEditOptionDate() {

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STRIKE_OUT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), MID,
                                                     VALIDATE_MAKE_AN_ORDER);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDate())
                .isEqualTo(NO);
        }

        @Test
        void shouldReturnYesForJudgeApproveEditOptionDate() {

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STAY_THE_CLAIM),
                                                          (GeneralApplicationTypes.EXTEND_TIME));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), MID,
                                                     VALIDATE_MAKE_AN_ORDER);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDate())
                .isEqualTo(YES);
        }

        @Test
        void shouldReturnCorrectDirectionsText() {

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STAY_THE_CLAIM),
                                                          (GeneralApplicationTypes.EXTEND_TIME));

            CallbackParams params = callbackParamsOf(getDirectionsText(types, NO, NO), MID,
                                                     VALIDATE_MAKE_AN_ORDER);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDirectionsText()).isEqualTo("Test directionText");
        }

        @Test
        void shouldAddCorrectDirectionsText() {

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STAY_THE_CLAIM),
                                                          (GeneralApplicationTypes.EXTEND_TIME));

            CallbackParams params = callbackParamsOf(getMakeAnOrder(types, NO, NO), MID,
                                                     VALIDATE_MAKE_AN_ORDER);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDirectionsText()).isEqualTo(PERSON_NOT_NOTIFIED_TEXT);
        }

        @Test
        void shouldReturnNOForJudgeApproveEditOptionParty() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), MID,
                                                     VALIDATE_MAKE_AN_ORDER);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(NO);
        }

        @Test
        void shouldReturnNOForjudgeApproveEditOptionDoc() {

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.EXTEND_TIME), (GeneralApplicationTypes.SUMMARY_JUDGEMENT));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), MID,
                                                     VALIDATE_MAKE_AN_ORDER);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(YES);
        }

        @Test
        void testAboutToStartForNotifiedApplication() {
            String expectedRecitalText = "<Title> <Name> \n"
                + "Upon reading the application of Claimant dated 15 January 22 and upon the "
                + "application of ApplicantPartyName dated %s and upon considering the information "
                + "provided by the parties";

            CallbackParams params = callbackParamsOf(getNotifiedApplication(), MID, VALIDATE_MAKE_AN_ORDER);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getJudgeRecitalText())
                .isEqualTo(String.format(expectedRecitalText, DATE_FORMATTER.format(LocalDate.now())));
            assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
        }
    }

    @Nested
    class MidEventForRespondToDirectionsDateValidity {

        private static final String VALIDATE_MAKE_DECISION_SCREEN = "validate-make-decision-screen";
        public static final String RESPOND_TO_DIRECTIONS_DATE_REQUIRED = "The date, by which the response to direction"
                + " should be given, is required.";
        public static final String RESPOND_TO_DIRECTIONS_DATE_IN_PAST = "The date, by which the response to direction"
                + " should be given, cannot be in past.";

        @Test
        void shouldNotCauseAnyErrors_whenApplicationDetailsNotProvided() {
            CaseData caseData = CaseDataBuilder.builder().build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnErrors_whenApplicationIsUrgentButConsiderationDateIsNotProvided() {
            CaseData caseData = getApplication_MakeDecision_GiveDirections(GIVE_DIRECTIONS_WITHOUT_HEARING, null);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(RESPOND_TO_DIRECTIONS_DATE_REQUIRED);
        }

        @Test
        void shouldReturnErrors_whenUrgencyConsiderationDateIsInPastForUrgentApplication() {
            CaseData caseData = getApplication_MakeDecision_GiveDirections(GIVE_DIRECTIONS_WITHOUT_HEARING,
                    LocalDate.now().minusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(RESPOND_TO_DIRECTIONS_DATE_IN_PAST);
        }

        @Test
        void shouldNotCauseAnyErrors_whenUrgencyConsiderationDateIsInFutureForUrgentApplication() {
            CaseData caseData = getApplication_MakeDecision_GiveDirections(GIVE_DIRECTIONS_WITHOUT_HEARING,
                    LocalDate.now().plusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldNotCauseAnyErrors_whenApplicationIsNotUrgentAndConsiderationDateIsNotProvided() {
            CaseData caseData = getApplication_MakeDecision_GiveDirections(APPROVE_OR_EDIT,
                    LocalDate.now().minusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        private CaseData getApplication_MakeDecision_GiveDirections(GAJudgeMakeAnOrderOption orderOption,
                                                                    LocalDate directionsResponseByDate) {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .parentClaimantIsApplicant(YES)
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().build())
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
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(orderOption)
                                               .judgeApproveEditOptionDate(LocalDate.now().plusDays(1))
                                               .directionsResponseByDate(directionsResponseByDate).build())
                .build();
        }
    }

    @Nested
    class MidEventForRequestMoreInfoScreenDateValidity {

        LocalDateTime responseDate = LocalDateTime.now();
        LocalDateTime deadline = LocalDateTime.now().plusDays(5);

        @BeforeEach
        void setup() {
            when(time.now()).thenReturn(responseDate);
            when(deadlinesCalculator.calculateApplicantResponseDeadline(
                any(LocalDateTime.class), any(Integer.class))).thenReturn(deadline);
        }

        private static final String VALIDATE_REQUEST_MORE_INFO_SCREEN = "validate-request-more-info-screen";
        public static final String REQUESTED_MORE_INFO_BY_DATE_REQUIRED = "The date, by which the applicant must "
                + "respond, is required.";
        public static final String REQUESTED_MORE_INFO_BY_DATE_IN_PAST = "The date, by which the applicant must "
                + "respond, cannot be in past.";

        @Test
        void shouldNotCauseAnyErrors_whenApplicationDetailsNotProvided() {
            CaseData caseData = CaseDataBuilder.builder().build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnErrors_whenRequestedMoreInfoAndTheDateIsNull() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION, null, NO);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(REQUESTED_MORE_INFO_BY_DATE_REQUIRED);
        }

        @Test
        void shouldReturnErrors_whenRequestedMoreInfoAndTheDateIsInPast() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION,
                    LocalDate.now().minusDays(1), NO);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(REQUESTED_MORE_INFO_BY_DATE_IN_PAST);
        }

        @Test
        void shouldNotReturnErrors_whenRequestedMoreInfoAndTheDateIsInFuture() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION,
                    LocalDate.now().plusDays(1), YES);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldNotCauseAnyErrors_whenApplicationIsNotUrgentAndConsiderationDateIsNotProvided() {
            CaseData caseData = getApplication_RequestMoreInformation(null,
                    LocalDate.now(), NO);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldNotReturnErrors_DeadlineForMoreInfoSubmissionIsPopulated() {
            CaseData caseData = getApplication_RequestMoreInformation(SEND_APP_TO_OTHER_PARTY,
                                                                      LocalDate.now().plusDays(1), YES);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response.getErrors()).isEmpty();
            assertThat(responseCaseData.getJudicialDecisionRequestMoreInfo().getDeadlineForMoreInfoSubmission())
                .isEqualTo(deadline.toString());
        }

        private CaseData getApplication_RequestMoreInformation(GAJudgeRequestMoreInfoOption option,
                                                               LocalDate judgeRequestMoreInfoByDate, YesOrNo hasAgree) {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .judicialDecision(GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build())
                .parentClaimantIsApplicant(YES)
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(hasAgree).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().build())
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
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(APPROVE_OR_EDIT)
                                               .build())
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                     .requestMoreInfoOption(option)
                                                     .judgeRequestMoreInfoByDate(judgeRequestMoreInfoByDate)
                                                     .judgeRequestMoreInfoText("Test")
                                                     .deadlineForMoreInfoSubmission(LocalDateTime.now().plusDays(5))
                                                     .build())
                .build();
        }
    }

    @Nested
    class AboutToSubmitHandling {

        @Test
        void shouldSetUpReadyBusinessProcess() {
            CaseData caseData = getApplicationBusinessProcess();

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getMakeAppVisibleToRespondents().getMakeAppAvailableCheck() != null);
            assertThat(responseCaseData.getBusinessProcess().getStatus()).isEqualTo(BusinessProcessStatus.READY);
            assertThat(responseCaseData.getBusinessProcess().getCamundaEvent()).isEqualTo("JUDGE_MAKES_DECISION");
        }

        @Test
        void shouldSetUpReadyWhenPreferredTypeNotInPerson() {
            CaseData caseData = getApplicationWithPreferredTypeNotInPerson();

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getJudicialListForHearing().getHearingPreferredLocation() == null);
            assertThat(responseCaseData.getBusinessProcess().getStatus()).isEqualTo(BusinessProcessStatus.READY);
            assertThat(responseCaseData.getBusinessProcess().getCamundaEvent()).isEqualTo("JUDGE_MAKES_DECISION");
        }

        private CaseData getApplicationBusinessProcess() {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(LIST_FOR_A_HEARING).build())
                .makeAppVisibleToRespondents(GAMakeApplicationAvailableCheck.builder()
                                                 .makeAppAvailableCheck(getMakeAppVisible()).build())
                .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                            .hearingPreferencesPreferredType(GAJudicialHearingType.IN_PERSON)
                                            .hearingPreferredLocation(getLocationDynamicList()).build())
                .businessProcess(BusinessProcess
                                     .builder()
                                     .camundaEvent(CAMUNDA_EVENT)
                                     .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                                     .status(BusinessProcessStatus.FINISHED)
                                     .activityId(ACTIVITY_ID)
                                     .build())
                .build();
        }

        private CaseData getApplicationWithPreferredTypeNotInPerson() {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(LIST_FOR_A_HEARING).build())
                .makeAppVisibleToRespondents(GAMakeApplicationAvailableCheck.builder()
                                                 .makeAppAvailableCheck(getMakeAppVisible()).build())
                .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                            .hearingPreferencesPreferredType(GAJudicialHearingType.TELEPHONE)
                                            .build())
                .businessProcess(BusinessProcess
                                     .builder()
                                     .camundaEvent(CAMUNDA_EVENT)
                                     .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                                     .status(BusinessProcessStatus.FINISHED)
                                     .activityId(ACTIVITY_ID)
                                     .build())
                .build();
        }
    }

    @Nested
    class SubmittedCallbackHandling {

        @Test
        void callbackHandlingShouldResultInErrorIfTheGAJudicialDecisionIsNull() {
            CaseData caseData = getApplication(null, null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            Assertions.assertThrows(IllegalArgumentException.class, () -> handler.handle(params));
        }

        @Test
        void callbackHandlingForMakeAnOrder() {
            CaseData caseData = getApplication(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build(), null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response.getConfirmationHeader()).isEqualTo("# Your order has been made");
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><br/>");
        }

        @Test
        void callbackHandlingForListForHearing() {
            CaseData caseData = getApplication(GAJudicialDecision.builder()
                    .decision(LIST_FOR_A_HEARING).build(), null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response.getConfirmationHeader()).isEqualTo("# Your order has been made");
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><br/>");
        }

        @Test
        void callbackHandlingForWrittenRepresentaion() {
            CaseData caseData = getApplication(GAJudicialDecision.builder()
                    .decision(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS).build(), null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response.getConfirmationHeader()).isEqualTo("# Your order has been made");
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><br/>");
        }

        @Test
        void callbackHandlingForRequestInfoFromApplicant() {
            CaseData caseData = getApplication(
                GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build(),
                GAJudicialRequestMoreInfo.builder()
                    .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                    .judgeRequestMoreInfoText("Test")
                    .judgeRequestMoreInfoByDate(LocalDate.now()).build()
            );
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response.getConfirmationHeader()).isEqualTo("# You have requested more information");
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><p>The applicant will be notified. "
                                                                     + "They will need to provide a response by "
                                                                     + DATE_FORMATTER_SUBMIT_CALLBACK
                .format(LocalDate.now()) + "</p>");
        }

        @Test
        void callbackHandlingForRequestHearingDetailsFromOtherParty() {
            CaseData caseData = getApplication(
                    GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build(),
                    GAJudicialRequestMoreInfo.builder()
                            .requestMoreInfoOption(SEND_APP_TO_OTHER_PARTY)
                            .deadlineForMoreInfoSubmission(LocalDateTime.now())
                            .build());

            LocalDateTime submissionEndDate = caseData.getJudicialDecisionRequestMoreInfo()
                .getDeadlineForMoreInfoSubmission();

            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response.getConfirmationHeader()).isEqualTo("# You have requested a response");
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><p>The parties will be notified. "
                    + "They will need to provide a response by "
                    + DATE_FORMATTER_SUBMIT_CALLBACK.format(submissionEndDate) + "</p>");
        }

        @Test
        void callbackHandlingForRequestMoreInfoWithNullGAJudicialRequestMoreInfo() {
            CaseData caseData = getApplication(
                    GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build(),
                    null);
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            Assertions.assertThrows(IllegalArgumentException.class, () -> handler.handle(params));
        }

        private CaseData getApplication(GAJudicialDecision decision, GAJudicialRequestMoreInfo moreInfo) {
            List<GeneralApplicationTypes> types = List.of(
                    (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            CaseData.CaseDataBuilder builder = CaseData.builder();
            if (decision != null && REQUEST_MORE_INFO.equals(decision.getDecision())) {
                builder.judicialDecisionRequestMoreInfo(moreInfo);
            }
            return builder
                    .parentClaimantIsApplicant(YES)
                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                    .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
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
                    .judicialDecision(decision)
                    .build();
        }
    }

    public CaseData getDirectionOrderApplnAndResp(List<GeneralApplicationTypes> types, YesOrNo vulQuestion,
                                                YesOrNo hasRespondentResponseVul) {

        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicantResponses())
                                          .build())
            .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YES).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .vulnerabilityQuestionsYesOrNo(vulQuestion)
                                          .vulnerabilityQuestion("dummy")
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicantResponses())
                                          .hearingPreferredLocation(getLocationDynamicList())
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

    public CaseData getDirectionsText(List<GeneralApplicationTypes> types, YesOrNo vulQuestion,
                                                  YesOrNo hasRespondentResponseVul) {

        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicantResponses())
                                          .build())
            .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YES).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                                           .directionsText("Test directionText")
                                           .build())
            .build();
    }

    public CaseData getMakeAnOrder(List<GeneralApplicationTypes> types, YesOrNo vulQuestion,
                                      YesOrNo hasRespondentResponseVul) {

        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicantResponses())
                                          .build())
            .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YES).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                                           .build())
            .build();
    }

    public CaseData getHearingOrderApplnAndResp(List<GeneralApplicationTypes> types, YesOrNo vulQuestion,
                                                 YesOrNo hasRespondentResponseVul) {

        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .hearingDuration(GAHearingDuration.HOUR_1)
                                    .supportRequirement(getApplicantResponses())
                                    .build())
            .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YES).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .vulnerabilityQuestionsYesOrNo(vulQuestion)
                                          .vulnerabilityQuestion("dummy")
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicantResponses())
                                          .hearingPreferredLocation(getLocationDynamicList())
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

    public CaseData getHearingOrderApplnAndResp1and2(List<GeneralApplicationTypes> types, YesOrNo vulQuestion,
                                                YesOrNo hasRespondentResponseVul, YesOrNo hasRespondentResponseVul2) {
        List<Element<GASolicitorDetailsGAspec>> respondentSolicitors = new ArrayList<>();
        respondentSolicitors
            .add(element(GASolicitorDetailsGAspec.builder().id("1L").build()));
        respondentSolicitors
            .add(element(GASolicitorDetailsGAspec.builder().id("2L").build()));

        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .hearingDuration(GAHearingDuration.HOUR_1)
                                    .supportRequirement(getApplicantResponses())
                                    .build())
            .respondentsResponses(getRespondentResponses1nad2(hasRespondentResponseVul, hasRespondentResponseVul2, YES,
                                                              YES))
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .generalAppRespondentSolicitors(respondentSolicitors)
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .vulnerabilityQuestionsYesOrNo(vulQuestion)
                                          .vulnerabilityQuestion("dummy")
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicant1Responses())
                                          .hearingPreferredLocation(getLocationDynamicList())
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

    public CaseData getHearingOrderAppForCourtLocationPreference(List<GeneralApplicationTypes> types,
                                                                 YesOrNo hasApplPreferLocation,
                                                                 YesOrNo hasResp1PreferLocation,
                                                                 YesOrNo hasResp2PreferLocation) {
        List<Element<GASolicitorDetailsGAspec>> respondentSolicitors = new ArrayList<>();
        respondentSolicitors
            .add(element(GASolicitorDetailsGAspec.builder().id("1L").build()));
        respondentSolicitors
            .add(element(GASolicitorDetailsGAspec.builder().id("2L").build()));

        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .hearingDuration(GAHearingDuration.HOUR_1)
                                    .supportRequirement(getApplicantResponses())
                                    .build())
            .respondentsResponses(getRespondentResponses1nad2(YES, YES, hasResp1PreferLocation, hasResp2PreferLocation))
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .generalAppRespondentSolicitors(respondentSolicitors)
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .vulnerabilityQuestionsYesOrNo(YES)
                                          .vulnerabilityQuestion("dummy")
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicant1Responses())
                                          .hearingPreferredLocation(hasApplPreferLocation == YES
                                                                        ? getLocationDynamicList() : null)
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

    public List<Element<GARespondentResponse>> getRespodentResponses(YesOrNo vulQuestion) {

        List<GAHearingSupportRequirements> respSupportReq = new ArrayList<>();
        respSupportReq
            .add(GAHearingSupportRequirements.OTHER_SUPPORT);
        respSupportReq
            .add(GAHearingSupportRequirements.HEARING_LOOPS);

        List<Element<GARespondentResponse>> respondentsResponses = new ArrayList<>();
        respondentsResponses
            .add(element(GARespondentResponse.builder()
                             .gaHearingDetails(GAHearingDetails.builder()
                                                   .vulnerabilityQuestionsYesOrNo(vulQuestion)
                                                   .vulnerabilityQuestion("dummy")
                                                   .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                                   .hearingDuration(GAHearingDuration.HOUR_1)
                                                   .supportRequirement(respSupportReq)
                                                   .hearingPreferredLocation(getLocationDynamicList())
                                                   .build()).build()
            ));

        return respondentsResponses;
    }

    public List<Element<GARespondentResponse>> getRespondentResponses1nad2(YesOrNo vulQuestion1, YesOrNo vulQuestion2,
                                                                          YesOrNo hasResp1PreferLocation,
                                                                          YesOrNo hasResp2PreferLocation) {

        List<GAHearingSupportRequirements> respSupportReq1 = new ArrayList<>();
        respSupportReq1
            .add(GAHearingSupportRequirements.OTHER_SUPPORT);

        List<GAHearingSupportRequirements> respSupportReq2 = new ArrayList<>();
        respSupportReq2
            .add(GAHearingSupportRequirements.LANGUAGE_INTERPRETER);

        List<Element<GARespondentResponse>> respondentsResponses = new ArrayList<>();
        respondentsResponses
            .add(element(GARespondentResponse.builder()
                             .gaHearingDetails(GAHearingDetails.builder()
                                                   .vulnerabilityQuestionsYesOrNo(vulQuestion1)
                                                   .vulnerabilityQuestion("dummy1")
                                                   .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                                   .hearingDuration(GAHearingDuration.HOUR_1)
                                                   .supportRequirement(respSupportReq1)
                                                   .hearingPreferredLocation(hasResp1PreferLocation == YES
                                                                                 ? getLocationDynamicList() : null)
                                                   .build())
                             .gaRespondentDetails("1L").build()));
        respondentsResponses
            .add(element(GARespondentResponse.builder()
                             .gaHearingDetails(GAHearingDetails.builder()
                                                   .vulnerabilityQuestionsYesOrNo(vulQuestion2)
                                                   .vulnerabilityQuestion("dummy2")
                                                   .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                                   .hearingDuration(GAHearingDuration.MINUTES_30)
                                                   .supportRequirement(respSupportReq2)
                                                   .hearingPreferredLocation(hasResp2PreferLocation == YES
                                                                                 ? getLocationDynamicList() : null)
                                                   .build())
                             .gaRespondentDetails("2L").build()));

        return respondentsResponses;
    }

    public List<MakeAppAvailableCheckGAspec> getMakeAppVisible() {
        List<MakeAppAvailableCheckGAspec> applMakeVisible = new ArrayList<>();
        applMakeVisible
            .add(MakeAppAvailableCheckGAspec.ConsentAgreementCheckBox);
        return applMakeVisible;
    }

    public List<GAHearingSupportRequirements> getApplicantResponses() {
        List<GAHearingSupportRequirements> applSupportReq = new ArrayList<>();
        applSupportReq
            .add(GAHearingSupportRequirements.HEARING_LOOPS);
        applSupportReq
            .add(GAHearingSupportRequirements.OTHER_SUPPORT);

        return applSupportReq;
    }

    public List<GAHearingSupportRequirements> getApplicant1Responses() {
        List<GAHearingSupportRequirements> applSupportReq = new ArrayList<>();
        applSupportReq
            .add(GAHearingSupportRequirements.HEARING_LOOPS);

        return applSupportReq;
    }

    public CaseData getNotifiedApplication() {

        YesOrNo hasRespondentResponseVul = YES;

        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .generalAppDetailsOfOrder("Draft order text entered by applicant.")
            .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
            .generalAppHearingDetails(GAHearingDetails.builder()
                                          .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                          .hearingDuration(GAHearingDuration.HOUR_1)
                                          .supportRequirement(getApplicantResponses())
                                          .build())
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

    public GAJudicialMakeAnOrder getJudicialMakeAnOrder(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData.getJudicialDecisionMakeOrder();
    }

    public YesOrNo getApplicationIsCloakedStatus(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData.getApplicationIsCloaked();
    }

    public DynamicList getLocationDynamicList() {
        DynamicListElement location1 = DynamicListElement.builder()
            .code(UUID.randomUUID()).label("ABCD - RG0 0AL").build();
        DynamicListElement location2 = DynamicListElement.builder()
            .code(UUID.randomUUID()).label("PQRS - GU0 0EE").build();
        DynamicListElement location3 = DynamicListElement.builder()
            .code(UUID.randomUUID()).label("WXYZ - EW0 0HE").build();
        DynamicListElement location4 = DynamicListElement.builder()
            .code(UUID.randomUUID()).label("LMNO - NE0 0BH").build();

        return DynamicList.builder()
            .listItems(List.of(location1, location2, location3, location4))
            .value(location1).build();
    }
}

