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
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DeadlinesCalculator;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionWrittenRepService;
import uk.gov.hmcts.reform.civil.service.Time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
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

            String expecetedJudicialTimeEstimateText = " Both applicant and respondent estimate it would take %s.";
            String expecetedJudicialPreferrenceText = " Both applicant and respondent prefer %s.";

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

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
                .isEqualTo(getJudgeHearingSupportReqText(getHearingOrderApplnAndResp(types, NO, YES), YES));

        }

        @Test
        void shouldReturnEmptyStringForNullSupportReq() {

            List<Element<GARespondentResponse>> respondentResponse = null;

            GAUrgencyRequirement urgentApp = null;

            CallbackParams params = callbackParamsOf(getCaseDateWithNoSupportReq(respondentResponse,
                                                                                 urgentApp), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudgesHearingListGAspec responseCaseData = getJudicialHearingOrder(response);

            assertThat(responseCaseData.getJudgeHearingTimeEstimateText1())
                .isEqualTo(StringUtils.EMPTY);

            assertThat(responseCaseData.getHearingPreferencesPreferredTypeLabel1())
                .isEqualTo(StringUtils.EMPTY);

            assertThat(responseCaseData.getJudgeHearingSupportReqText1())
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

            List<Element<GARespondentResponse>> respondentResponse = getRespodentResponses(hasRespondentResponseVul);

            CallbackParams params = callbackParamsOf(getCaseDateWithNoSupportReq(respondentResponse,
                                                                                 urgentApp), ABOUT_TO_START);
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
        void testAboutToStartForNotifiedApplication() {
            String expectedRecitalText = "Upon reading the application of Claimant dated 15 January 22 and upon the "
                    + "application of ApplicantPartyName dated %s and upon considering the information "
                    + "provided by the parties";

            CallbackParams params = callbackParamsOf(getNotifiedApplication(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(NO);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(expectedRecitalText,
                    DATE_FORMATTER.format(LocalDate.now())));
            assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
        }

        @Test
        void testAboutToStartForCloakedApplication() {
            String expectedRecitalText = "Upon reading the application of Claimant dated 15 January 22 and upon the "
                    + "application of ApplicantPartyName dated %s and upon considering the information "
                    + "provided by the parties";

            CallbackParams params = callbackParamsOf(getCloakedApplication(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(YES);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(expectedRecitalText,
                    DATE_FORMATTER.format(LocalDate.now())));
            assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
        }

        @Test
        void testAboutToStartForDefendant_judgeRecitalText() {
            String expectedRecitalText = "Upon reading the application of Defendant dated 15 January 22 and upon the "
                    + "application of ApplicantPartyName dated %s and upon considering the information "
                    + "provided by the parties";

            CallbackParams params = callbackParamsOf(getApplicationByParentCaseDefendant(), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(NO);
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(expectedRecitalText,
                    DATE_FORMATTER.format(LocalDate.now())));
            assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
        }

        @Test
        void testAboutToStartForDefendant_orderText() {
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

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STAY_THE_CLAIM),
                                                          (GeneralApplicationTypes.EXTEND_TIME));

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

            List<GeneralApplicationTypes> types = List.of((GeneralApplicationTypes.STRIKE_OUT),
                                                          (GeneralApplicationTypes.EXTEND_TIME));

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, NO), ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isNotNull();
            GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

            assertThat(makeAnOrder.getDisplayjudgeApproveEditOptionDoc())
                .isEqualTo(YES);

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

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(types, NO, YES), ABOUT_TO_START);
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
                .hearingDetailsResp(GAHearingDetails.builder()
                                        .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                        .hearingDuration(GAHearingDuration.HOUR_1)
                                        .supportRequirement(getApplicantResponses())
                                        .build())
                .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
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
                .hearingDetailsResp(GAHearingDetails.builder()
                                        .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                        .hearingDuration(GAHearingDuration.HOUR_1)
                                        .supportRequirement(getApplicantResponses())
                                        .build())
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
        void shouldReturnNullForJudgeGOSupportRequirement() {

            List<SupportRequirements> judgeSupportReqChoices = null;

            CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(judgeSupportReqChoices),
                                                     MID, VALIDATE_HEARING_ORDER_SCREEN);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response).isNotNull();

            assertThat(responseCaseData.getJudicialHearingGOHearingReqText())
                .isEqualTo("");

        }

        private CaseData getHearingOrderApplnAndResp(List<SupportRequirements> judgeSupportReqChoices) {

            YesOrNo hasRespondentResponseVul = NO;

            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                            .judicialSupportRequirement(judgeSupportReqChoices)
                                            .hearingPreferencesPreferredType(GAJudicialHearingType.VIDEO)
                                            .judicialTimeEstimate(GAHearingDuration.HOURS_2).build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .hearingDetailsResp(GAHearingDetails.builder()
                                        .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                        .hearingDuration(GAHearingDuration.HOUR_1)
                                        .supportRequirement(getApplicantResponses())
                                        .build())
                .respondentsResponses(getRespodentResponses(hasRespondentResponseVul))
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
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
            String expectedRecitalText = "Upon reading the application of Claimant dated 15 January 22 and upon the "
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
            CaseData caseData = getApplication_MakeDecision_GiveDirections(DISMISS_THE_APPLICATION,
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
                            .directionsText("ABC")
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
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION, null);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(REQUESTED_MORE_INFO_BY_DATE_REQUIRED);
        }

        @Test
        void shouldReturnErrors_whenRequestedMoreInfoAndTheDateIsInPast() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION,
                    LocalDate.now().minusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(REQUESTED_MORE_INFO_BY_DATE_IN_PAST);
        }

        @Test
        void shouldNotReturnErrors_whenRequestedMoreInfoAndTheDateIsInFuture() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION,
                    LocalDate.now().plusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldNotCauseAnyErrors_whenApplicationIsNotUrgentAndConsiderationDateIsNotProvided() {
            CaseData caseData = getApplication_RequestMoreInformation(null,
                    LocalDate.now().minusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldNotReturnErrors_DeadlineForMoreInfoSubmissionIsPopulated() {
            CaseData caseData = getApplication_RequestMoreInformation(SEND_APP_TO_OTHER_PARTY,
                                                                      LocalDate.now().plusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(response.getErrors()).isEmpty();
            assertThat(responseCaseData.getJudicialDecisionRequestMoreInfo().getDeadlineForMoreInfoSubmission())
                .isEqualTo(deadline.toString());
        }

        private CaseData getApplication_RequestMoreInformation(GAJudgeRequestMoreInfoOption option,
                                                               LocalDate judgeRequestMoreInfoByDate) {
            List<GeneralApplicationTypes> types = List.of(
                    (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                    .parentClaimantIsApplicant(YES)
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
                            .makeAnOrder(APPROVE_OR_EDIT)
                            .build())
                    .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                            .requestMoreInfoOption(option)
                            .judgeRequestMoreInfoByDate(judgeRequestMoreInfoByDate)
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

            assertThat(responseCaseData.getBusinessProcess().getStatus()).isEqualTo(BusinessProcessStatus.READY);
            assertThat(responseCaseData.getBusinessProcess().getCamundaEvent()).isEqualTo("JUDGE_MAKES_DECISION");
        }

        private CaseData getApplicationBusinessProcess() {
            List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
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
                            .judgeRequestMoreInfoByDate(LocalDate.now()).build());
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response.getConfirmationHeader()).isEqualTo("# You have requested more information");
            assertThat(response.getConfirmationBody()).isEqualTo("<br/><p>The applicant will be notified. "
                    + "They will need to provide a response by "
                    + DATE_FORMATTER_SUBMIT_CALLBACK.format(LocalDate.now()) + "</p>");
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

        @Test
        void callbackHandlingForRequestMoreInfoWithNullJudgeRequestMoreInfoByDate() {
            CaseData caseData = getApplication(
                    GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build(),
                    GAJudicialRequestMoreInfo.builder()
                            .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                            .judgeRequestMoreInfoByDate(null).build());
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

    public CaseData getHearingOrderApplnAndResp(List<GeneralApplicationTypes> types, YesOrNo vulQuestion,
                                                 YesOrNo hasRespondentResponseVul) {

        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .hearingDuration(GAHearingDuration.HOUR_1)
                                    .supportRequirement(getApplicantResponses())
                                    .build())
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
                                                   .build()).build()
            ));

        return respondentsResponses;
    }

    public List<GAHearingSupportRequirements> getApplicantResponses() {
        List<GAHearingSupportRequirements> applSupportReq = new ArrayList<>();
        applSupportReq
            .add(GAHearingSupportRequirements.HEARING_LOOPS);
        applSupportReq
            .add(GAHearingSupportRequirements.OTHER_SUPPORT);

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
}

