package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.ccd.model.SolicitorDetails;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUnavailabilityDates;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@SpringBootTest(classes = {
    CaseDetailsConverter.class,
    RespondToApplicationHandler.class,
    JacksonAutoConfiguration.class,
},
    properties = {"reference.database.enabled=false"})
public class RespondToApplicationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    RespondToApplicationHandler handler;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CaseDetailsConverter caseDetailsConverter;

    @MockBean
    ParentCaseUpdateHelper parentCaseUpdateHelper;

    List<Element<SolicitorDetails>> respondentSols = new ArrayList<>();

    List<Element<GARespondentResponse>> respondentsResponses = new ArrayList<>();

    private static final String CAMUNDA_EVENT = "INITIATE_GENERAL_APPLICATION";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    private static final String CONFIRMATION_MESSAGE = "<br/><p> In relation to the following application(s): </p>"
        + "<ul> <li>Summary judgment</li> </ul>"
        + " <p> The application and your response will be reviewed by a Judge. </p> ";
    private static final String ERROR = "The General Application has already received a response.";
    public static final String TRIAL_DATE_FROM_REQUIRED = "Please enter the Date from if the trial has been fixed";
    public static final String INVALID_TRIAL_DATE_RANGE = "Trial Date From cannot be after Trial Date to. "
        + "Please enter valid range.";
    public static final String UNAVAILABLE_DATE_RANGE_MISSING = "Please provide at least one valid Date from if you "
        + "cannot attend hearing within next 3 months.";
    public static final String UNAVAILABLE_FROM_MUST_BE_PROVIDED = "If you selected option to be unavailable then "
        + "you must provide at least one valid Date from";
    public static final String INVALID_UNAVAILABILITY_RANGE = "Unavailability Date From cannot be after "
        + "Unavailability Date to. Please enter valid range.";
    public static final String INVALID_TRAIL_DATE_FROM_BEFORE_TODAY = "Trail date from must not be before today.";
    public static final String INVALID_UNAVAILABLE_DATE_FROM_BEFORE_TODAY = "Unavailability date from must not"
        + " be before today.";
    public static final LocalDate TRIAL_DATE_FROM_INVALID = LocalDate.of(2022, 3, 1);
    public static final LocalDate TRIAL_DATE_FROM_AFTER_INVALID = TRIAL_DATE_FROM_INVALID.plusDays(10L);
    public static final LocalDate TRIAL_DATE_TO_BEFORE_INVALID = TRIAL_DATE_FROM_INVALID.minusDays(10L);

    public static final LocalDate UNAVAILABILITY_DATE_FROM_INVALID = LocalDate.of(2022, 3, 1);
    public static final LocalDate UNAVAILABILITY_DATE_TO_INVALID = TRIAL_DATE_FROM_INVALID.minusDays(10L);

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(RESPOND_TO_APPLICATION);
    }

    @Test
    void buildResponseConfirmationReturnsCorrectMessage() {
        CallbackParams params = callbackParamsOf(getCase(), CallbackType.SUBMITTED);
        var response = (SubmittedCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getConfirmationBody()).isEqualTo(CONFIRMATION_MESSAGE);
    }

    @Test
    void generalAppRespondent1RepGivesCorrectValueWhenInvoked() {
        YesOrNo repAgreed = getCase().getGeneralAppRespondent1Representative()
            .getGeneralAppRespondent1Representative();
        assertThat(repAgreed).isEqualTo(YES);
    }

    @Test
    void aboutToStartCallbackChecksApplicationStateBeforeProceeding() {
        CallbackParams params = callbackParamsOf(getCase(), CallbackType.ABOUT_TO_START);
        List<String> errors = new ArrayList<>();
        errors.add(ERROR);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void midCallBackRaisesCorrectErrorForInvalidTrailDateRange() {
        CallbackParams params = getParams(0);
        List<String> errors = new ArrayList<>();
        errors.add(INVALID_TRIAL_DATE_RANGE);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void midCallBackRaisesCorrectErrorForInvalidTrailDateFromBeforeToday() {
        CallbackParams params = getParams(1);
        List<String> errors = new ArrayList<>();
        errors.add(INVALID_TRAIL_DATE_FROM_BEFORE_TODAY);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void midCallBackRaisesCorrectErrorForTrailDateFromNotAvailable() {
        CallbackParams params = getParams(2);
        List<String> errors = new ArrayList<>();
        errors.add(TRIAL_DATE_FROM_REQUIRED);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void midCallBackRaisesCorrectErrorForWrongRangeForUnavailableDates() {
        CallbackParams params = getParams(3);
        List<String> errors = new ArrayList<>();
        errors.add(INVALID_UNAVAILABILITY_RANGE);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void midCallBackRaisesCorrectErrorForDateBeforeToday() {
        CallbackParams params = getParams(4);
        List<String> errors = new ArrayList<>();
        errors.add(INVALID_UNAVAILABLE_DATE_FROM_BEFORE_TODAY);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void midCallBackRaisesCorrectErrorWhenUnavailableDatesAreNull() {
        CallbackParams params = getParams(5);
        List<String> errors = new ArrayList<>();
        errors.add(UNAVAILABLE_FROM_MUST_BE_PROVIDED);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void midCallBackRaisesCorrectErrorWhenUnavailableDateFromIsNull() {
        CallbackParams params = getParams(6);
        List<String> errors = new ArrayList<>();
        errors.add(UNAVAILABLE_DATE_RANGE_MISSING);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    private CallbackParams getParams(int trialRanges) {
        switch (trialRanges) {
            case 0:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCaseWithInvalidTrailDateRange())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
            case 1:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCaseWithInvalidDateToRange())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
            case 2:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCaseWithNullFromAndToDate())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
            case 3:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCaseWithUnavailableDates())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
            case 4:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCaseWithUnavailableDatesBeforeToday())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
            case 5:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCaseWithNullUnavailableDates())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
            case 6:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCaseWithNullUnavailableDateFrom())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
            default :
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCase())
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
        }
    }

    @Test
    void shouldReturn_Awaiting_Respondent_Response_1Def_2Responses() {

        respondentSols.add(element(SolicitorDetails.builder().caseRole("role").build()));
        respondentsResponses.add(element(GARespondentResponse.builder().generalAppRespondent1Representative(YES).build()
        ));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("AWAITING_RESPONDENT_RESPONSE");
    }

    @Test
    void shouldReturn_Application_Submitted_Awaiting_Judicial_Decision_2Def_2Responses() {

        SolicitorDetails solicitorDetails = SolicitorDetails.builder().caseRole("role").build();

        Collections.addAll(respondentSols, element(solicitorDetails), element(solicitorDetails));

        respondentsResponses.add(element(GARespondentResponse.builder()
                                             .generalAppRespondent1Representative(YES).build()));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION");
    }

    @Test
    void shouldReturn_Application_Submitted_Awaiting_Judicial_Decision_1Def_1Response() {

        SolicitorDetails solicitorDetails = SolicitorDetails.builder().caseRole("role").build();

        Collections.addAll(respondentSols, element(solicitorDetails));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION");
    }

    @Test
    void shouldReturn_Awaiting_Respondent_Response_2Def_1Response() {

        SolicitorDetails solicitorDetails = SolicitorDetails.builder().caseRole("role").build();

        Collections.addAll(respondentSols, element(solicitorDetails), element(solicitorDetails));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("AWAITING_RESPONDENT_RESPONSE");
    }

    @Test
    void shouldReturn_Awaiting_Respondent_Response_For_NoDef_NoResponse() {

        CaseData caseData = getCase();

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo("AWAITING_RESPONDENT_RESPONSE");
    }

    private CaseData getCaseWithNullUnavailableDateFrom() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .unavailableTrialRequiredYesOrNo(YES)
                                    .generalAppUnavailableDates(null)
                                    .build())
            .build();
    }

    private CaseData getCaseWithNullUnavailableDates() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .unavailableTrialRequiredYesOrNo(YES)
                                    .generalAppUnavailableDates(getUnavailableNullDateList())
                                    .build())
            .build();
    }

    private CaseData getCaseWithInvalidTrailDateRange() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .trialRequiredYesOrNo(YES)
                                    .trialDateFrom(TRIAL_DATE_FROM_INVALID)
                                    .trialDateTo(TRIAL_DATE_TO_BEFORE_INVALID)
                                    .build())
            .build();
    }

    private CaseData getCaseWithInvalidDateToRange() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .trialRequiredYesOrNo(YES)
                                    .trialDateFrom(TRIAL_DATE_FROM_INVALID)
                                    .trialDateTo(TRIAL_DATE_FROM_AFTER_INVALID)
                                    .build())
            .build();
    }

    private CaseData getCaseWithNullFromAndToDate() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .trialRequiredYesOrNo(YES)
                                    .trialDateFrom(null)
                                    .trialDateTo(null)
                                    .build())
            .build();
    }

    private CaseData getCaseWithUnavailableDates() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .unavailableTrialRequiredYesOrNo(YES)
                                    .generalAppUnavailableDates(getUnavailableDateList())
                                    .build())
            .build();
    }

    private CaseData getCaseWithUnavailableDatesBeforeToday() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .unavailableTrialRequiredYesOrNo(YES)
                                    .generalAppUnavailableDates(getUnavailableDateBeforeToday())
                                    .build())
            .build();
    }

    private List<Element<GAUnavailabilityDates>> getUnavailableNullDateList() {
        GAUnavailabilityDates invalidDates = GAUnavailabilityDates.builder()
            .unavailableTrialDateFrom(null)
            .unavailableTrialDateTo(null)
            .build();
        return wrapElements(invalidDates);
    }

    private List<Element<GAUnavailabilityDates>> getUnavailableDateList() {
        GAUnavailabilityDates invalidDates = GAUnavailabilityDates.builder()
            .unavailableTrialDateFrom(UNAVAILABILITY_DATE_FROM_INVALID)
            .unavailableTrialDateTo(UNAVAILABILITY_DATE_TO_INVALID)
            .build();
        return wrapElements(invalidDates);
    }

    private List<Element<GAUnavailabilityDates>> getUnavailableDateBeforeToday() {
        GAUnavailabilityDates invalidDates = GAUnavailabilityDates.builder()
            .unavailableTrialDateFrom(UNAVAILABILITY_DATE_FROM_INVALID)
            .build();
        return wrapElements(invalidDates);
    }

    private CaseData getCase() {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
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

    private CaseData getCase(List<Element<SolicitorDetails>> defendantSolicitors,
                             List<Element<GARespondentResponse>> respondentsResponses) {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
            .defendantSolicitors(defendantSolicitors)
            .respondentsResponses(respondentsResponses)
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
