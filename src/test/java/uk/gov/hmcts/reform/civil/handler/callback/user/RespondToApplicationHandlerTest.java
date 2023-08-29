package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.GADebtorPaymentPlanGAspec;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.GARespondentDebtorOfferOptionsGAspec;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentDebtorOfferGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUnavailabilityDates;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.VARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.model.common.DynamicList.fromList;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@SpringBootTest(classes = {
    CaseDetailsConverter.class,
    RespondToApplicationHandler.class,
    JacksonAutoConfiguration.class,
    AssignCategoryId.class
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
    private FeatureToggleService featureToggleService;
    @MockBean
    IdamClient idamClient;

    @MockBean
    ParentCaseUpdateHelper parentCaseUpdateHelper;

    @MockBean
    protected GeneralAppLocationRefDataService locationRefDataService;
    List<Element<Document>> documents = new ArrayList<>();
    @BeforeEach
        public void setUp() throws IOException {

        when(idamClient.getUserDetails(anyString())).thenReturn(UserDetails.builder()
                                                                    .id(STRING_CONSTANT)
                                                                    .build());
        Document document = Document.builder().documentUrl("url").documentFileName("file").build();
        documents.add(ElementUtils.element(document));
    }

    List<Element<GARespondentResponse>> respondentsResponses = new ArrayList<>();

    private static final String STRING_CONSTANT = "1234";
    private static final String CAMUNDA_EVENT = "INITIATE_GENERAL_APPLICATION";
    private static final String DUMMY_EMAIL = "test@gmail.com";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    private static final String CONFIRMATION_MESSAGE = "<br/><p> In relation to the following application(s): </p>"
        + "<ul> <li>Summary judgment</li> </ul>"
        + " <p> The application and your response will be reviewed by a Judge. </p> ";
    private static final String ERROR = "The General Application has already received a response.";
    private static final String RESPONDENT_ERROR = "The application has already been responded to.";
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
    public static final String PAYMENT_DATE_CANNOT_BE_IN_PAST =
        "The date entered cannot be in the past.";

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(RESPOND_TO_APPLICATION);
    }

    @Test
    void buildResponseConfirmationReturnsCorrectMessage() {
        CallbackParams params = callbackParamsOf(getCase(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION),
                                                 CallbackType.SUBMITTED);
        var response = (SubmittedCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getConfirmationBody()).isEqualTo(CONFIRMATION_MESSAGE);
    }

    @Test
    void generalAppRespondent1RepGivesCorrectValueWhenInvoked() {
        YesOrNo repAgreed = getCase(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .getGeneralAppRespondent1Representative().getGeneralAppRespondent1Representative();
        assertThat(repAgreed).isEqualTo(YES);
    }

    @Test
    void aboutToStartCallbackChecksApplicationStateBeforeProceeding() {

        List<LocationRefData> locations = new ArrayList<>();
        locations.add(LocationRefData.builder().siteName("siteName").courtAddress("court Address").postcode("post code")
                          .courtName("Court Name").region("Region").build());
        when(locationRefDataService.getCourtLocations(any())).thenReturn(locations);

        CallbackParams params = callbackParamsOf(getCase(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION),
                                                 CallbackType.ABOUT_TO_START);
        List<String> errors = new ArrayList<>();
        errors.add(ERROR);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);
    }

    @Test
    void aboutToStartCallbackChecksRespondendResponseBeforeProceeding() {

        List<LocationRefData> locations = new ArrayList<>();
        locations.add(LocationRefData.builder().siteName("siteName").courtAddress("court Address").postcode("post code")
                          .courtName("Court Name").region("Region").build());
        when(locationRefDataService.getCourtLocations(any())).thenReturn(locations);

        CaseData caseData = getCaseWithRespondentResponse();
        CaseData.CaseDataBuilder updateCaseData = caseData.toBuilder();
        List<GeneralApplicationTypes> types = List.of(SUMMARY_JUDGEMENT);
        updateCaseData.parentClaimantIsApplicant(YES)
            .generalAppType(GAApplicationType.builder().types(types).build()).build();

        CallbackParams params = callbackParamsOf(updateCaseData.build(), CallbackType.ABOUT_TO_START);
        List<String> errors = new ArrayList<>();
        errors.add(RESPONDENT_ERROR);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);

        CaseData data = objectMapper.convertValue(response.getData(), CaseData.class);
        assertThat(data.getGeneralAppVaryJudgementType()).isEqualTo(NO);
    }

    @Test
    void aboutToStartCallbackChecksRespondendResponseBeforeProceeding_VaryJudgement() {

        List<LocationRefData> locations = new ArrayList<>();
        locations.add(LocationRefData.builder().siteName("siteName").courtAddress("court Address").postcode("post code")
                          .courtName("Court Name").region("Region").build());
        when(locationRefDataService.getCourtLocations(any())).thenReturn(locations);

        CaseData caseData = getCaseWithRespondentResponse();
        CaseData.CaseDataBuilder updateCaseData = caseData.toBuilder();
        List<GeneralApplicationTypes> types = List.of(VARY_JUDGEMENT);
        updateCaseData.parentClaimantIsApplicant(NO)
            .generalAppType(GAApplicationType.builder().types(types).build()).build();

        CallbackParams params = callbackParamsOf(updateCaseData.build(), CallbackType.ABOUT_TO_START);
        List<String> errors = new ArrayList<>();
        errors.add(RESPONDENT_ERROR);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        assertThat(response.getErrors()).isEqualTo(errors);

        CaseData data = objectMapper.convertValue(response.getData(), CaseData.class);
        assertThat(data.getGeneralAppVaryJudgementType()).isEqualTo(YES);
    }

    @Test
    void aboutToStartCallbackAddsLocationDetails() {

        List<LocationRefData> locations = new ArrayList<>();
        locations.add(LocationRefData.builder().siteName("siteName").courtAddress("court Address").postcode("post code")
                          .courtName("Court Name").region("Region").build());
        when(locationRefDataService.getCourtLocations(any())).thenReturn(locations);

        CallbackParams params = callbackParamsOf(getCase(AWAITING_RESPONDENT_RESPONSE),
                                                 CallbackType.ABOUT_TO_START);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData data = objectMapper.convertValue(response.getData(), CaseData.class);

        assertThat(response.getErrors()).isEmpty();
        assertThat(data.getHearingDetailsResp()).isNotNull();
        DynamicList dynamicList = getLocationDynamicList(data);
        assertThat(dynamicList).isNotNull();
        assertThat(locationsFromDynamicList(dynamicList))
            .containsOnly("siteName - court Address - post code");
    }

    @Test
    void midCallBackValidateDebtorPaymentDatePastDateError() {
        CaseData caseData = getCase(AWAITING_RESPONDENT_RESPONSE);
        CaseData.CaseDataBuilder updateCaseData = caseData.toBuilder();
        List<GeneralApplicationTypes> types = List.of(VARY_JUDGEMENT);
        updateCaseData.parentClaimantIsApplicant(NO)
            .generalAppType(GAApplicationType.builder().types(types).build())
            .gaRespondentDebtorOffer(
            GARespondentDebtorOfferGAspec.builder().respondentDebtorOffer(
            GARespondentDebtorOfferOptionsGAspec.DECLINE)
                .paymentPlan(GADebtorPaymentPlanGAspec.PAYFULL)
                .paymentSetDate(LocalDate.now().minusDays(2)).build());

        CallbackParams params = callbackParamsOf(updateCaseData.build(),
                                                 CallbackType.MID, "validate-debtor-offer");


        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors().get(0)).isEqualTo(PAYMENT_DATE_CANNOT_BE_IN_PAST);
    }

    @Test
    void midCallBackValidateDebtorPaymentDateIsFuture() {
        CaseData caseData = getCase(AWAITING_RESPONDENT_RESPONSE);
        CaseData.CaseDataBuilder updateCaseData = caseData.toBuilder();
        List<GeneralApplicationTypes> types = List.of(VARY_JUDGEMENT);
        updateCaseData.generalAppType(GAApplicationType.builder().types(types).build())
            .gaRespondentDebtorOffer(
                GARespondentDebtorOfferGAspec.builder().respondentDebtorOffer(
                        GARespondentDebtorOfferOptionsGAspec.DECLINE)
                    .paymentPlan(GADebtorPaymentPlanGAspec.PAYFULL)
                    .paymentSetDate(LocalDate.now().plusDays(2)).build());

        CallbackParams params = callbackParamsOf(updateCaseData.build(),
                                                 CallbackType.MID, "validate-debtor-offer");


        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isEmpty();
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
            default:
                return CallbackParams.builder()
                    .type(CallbackType.MID)
                    .pageId("hearing-screen-response")
                    .caseData(getCase(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION))
                    .request(CallbackRequest.builder()
                                 .eventId("RESPOND_TO_APPLICATION")
                                 .build())
                    .build();
        }
    }

    @Test
    void shouldReturn_Awaiting_Respondent_Response_1Def_2Responses() {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response).isNotNull();
        CaseData responseCaseData = getResponseCaseData(response);
        assertThat(responseCaseData.getHearingDetailsResp()
                       .getHearingPreferredLocation().getListItems().size()).isEqualTo(1);
        assertThat(responseCaseData.getGeneralAppRespondent1Representative()).isEqualTo(null);
        assertThat(responseCaseData.getRespondentsResponses().size()).isEqualTo(1);
    }

    @Test
    void shouldReturn_Application_Submitted_Awaiting_Judicial_Decision_2Def_2Responses() {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));

        respondentsResponses.add(element(GARespondentResponse.builder()
                                             .generalAppRespondent1Representative(YES).build()));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response).isNotNull();

        CaseData responseCaseData = getResponseCaseData(response);
        assertThat(responseCaseData.getHearingDetailsResp()
                       .getHearingPreferredLocation().getListItems().size()).isEqualTo(1);
        assertThat(responseCaseData.getGeneralAppRespondent1Representative()).isEqualTo(null);
        assertThat(responseCaseData.getRespondentsResponses().size()).isEqualTo(2);

    }

    @Test
    void shouldReturn_Application_Submitted_Awaiting_Judicial_Decision_1Def_1Response() {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData responseData = objectMapper.convertValue(response.getData(), CaseData.class);
        assertThat(response).isNotNull();
    }

    @Test
    void shouldReturn_Application_Submitted_Awaiting_Judicial_Decision_1Def_1Response_test() {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));

        CaseData caseData = getCaseWithJudicialDecision(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData responseData = objectMapper.convertValue(response.getData(), CaseData.class);

        assertThat(response).isNotNull();
    }

    @Test
    void shouldReturn_Awaiting_Respondent_Response_2Def_1Response() {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));

        CaseData caseData = getCase(respondentSols, respondentsResponses);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
        CaseData responseCaseData = getResponseCaseData(response);
        assertThat(responseCaseData.getHearingDetailsResp()
                       .getHearingPreferredLocation().getListItems().size()).isEqualTo(1);
        assertThat(responseCaseData.getGeneralAppRespondent1Representative()).isEqualTo(null);
        assertThat(responseCaseData.getRespondentsResponses().size()).isEqualTo(1);
    }

    @Test
    void shouldReturn_Awaiting_Respondent_Response_For_NoDef_NoResponse() {

        CaseData caseData = getCase(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION);

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();
    }

    @Test
    void shouldReturn_Null_WhenPreferredTypeNotInPerson() {

        CaseData caseData = getCaseWithPreferredTypeInPersonLocationNull();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.parentClaimantIsApplicant(NO)
            .generalAppType(GAApplicationType.builder().types(List.of(SUMMARY_JUDGEMENT)).build()).build();

        Map<String, Object> dataMap = objectMapper.convertValue(caseDataBuilder.build(), new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        assertThat(response).isNotNull();
        assertThat(responseCaseData.getHearingDetailsResp().getHearingPreferredLocation()).isNull();
    }

    @Test
    void shouldReturn_No_WhenDebtorIsAcceptedByRespondent() {
        CaseData caseData = getCaseWithPreferredTypeInPersonLocationNull();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.parentClaimantIsApplicant(NO)
            .generalAppType(GAApplicationType.builder().types(List.of(VARY_JUDGEMENT)).build())
            .gaRespondentDebtorOffer(
            GARespondentDebtorOfferGAspec.builder().respondentDebtorOffer(
                    GARespondentDebtorOfferOptionsGAspec.DECLINE)
                .paymentPlan(GADebtorPaymentPlanGAspec.PAYFULL)
                .paymentSetDate(LocalDate.now().minusDays(2)).build())
            .generalAppRespondDebtorDocument(documents);

        Map<String, Object> dataMap = objectMapper.convertValue(caseDataBuilder.build(), new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        assertThat(response).isNotNull();
        assertThat(responseCaseData.getHearingDetailsResp().getHearingPreferredLocation()).isNull();
        assertThat(responseCaseData.getRespondentsResponses().get(0).getValue()
                       .getGeneralAppRespondent1Representative()).isEqualTo(NO);
        assertThat(responseCaseData.getGeneralAppRespondDebtorDocument()).isNull();
        assertThat(responseCaseData.getGaRespondDoc()).isNotNull();
        assertThat(responseCaseData.getGaRespondDoc()
                .get(0).getValue().getDocumentLink().getCategoryID())
                .isEqualTo(AssignCategoryId.APPLICATIONS);
        assertThat(responseCaseData.getGaRespondDoc()
                .get(0).getValue().getDocumentLink().getDocumentUrl())
                .isEqualTo("url");
        assertThat(responseCaseData.getGaRespondDoc()
                .get(0).getValue().getDocumentLink().getDocumentFileName())
                .isEqualTo("file");
    }

    @Test
    void shouldReturn_Yes_WhenDebtorIsAcceptedByRespondent() {
        CaseData caseData = getCaseWithPreferredTypeInPersonLocationNull();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.parentClaimantIsApplicant(NO)
            .generalAppType(GAApplicationType.builder().types(List.of(VARY_JUDGEMENT)).build())
            .gaRespondentDebtorOffer(
                GARespondentDebtorOfferGAspec.builder().respondentDebtorOffer(
                        GARespondentDebtorOfferOptionsGAspec.ACCEPT)
                    .paymentPlan(GADebtorPaymentPlanGAspec.PAYFULL).build());

        Map<String, Object> dataMap = objectMapper.convertValue(caseDataBuilder.build(), new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        assertThat(response).isNotNull();
        assertThat(responseCaseData.getHearingDetailsResp().getHearingPreferredLocation()).isNull();
        assertThat(responseCaseData.getRespondentsResponses().get(0).getValue()
                       .getGeneralAppRespondent1Representative()).isEqualTo(YES);
    }

    @Test
    void shouldReturn_Null_RespondentResponseAfterAddingToCollections() {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));

        CaseData caseData = getCase(respondentSols, respondentsResponses);
        DynamicList dynamicListTest = fromList(getSampleCourLocations());
        Optional<DynamicListElement> first = dynamicListTest.getListItems().stream().findFirst();
        first.ifPresent(dynamicListTest::setValue);

        CaseData updatedCaseData = caseData.toBuilder().hearingDetailsResp(GAHearingDetails.builder()
                                                                               .hearingPreferredLocation(
                                                                                   dynamicListTest)
                                                                               .hearingPreferencesPreferredType(
                                                                                   GAHearingType.IN_PERSON)
                                                                               .build()).build();
        caseData = updatedCaseData;

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        CallbackParams params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response).isNotNull();

        CaseData responseCaseData = getResponseCaseData(response);
        assertThat(responseCaseData.getGeneralAppRespondent1Representative()).isEqualTo(null);
        assertThat(responseCaseData.getRespondentsResponses().size()).isEqualTo(1);
    }

    private CaseData getResponseCaseData(AboutToStartOrSubmitCallbackResponse response) {
        return objectMapper.convertValue(response.getData(), CaseData.class);
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

    private CaseData getCaseWithPreferredTypeInPersonLocationNull() {
        return CaseData.builder()
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .hearingPreferredLocation(DynamicList.builder().build())
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

    private CaseData getCaseWithRespondentResponse() {

        respondentsResponses.add(element(GARespondentResponse.builder()
                                             .generalAppRespondent1Representative(NO)
                                             .gaRespondentDetails("1234").build()));
        return CaseData.builder()
            .respondentsResponses(respondentsResponses)
            .build();
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
                                 .camundaEvent(CAMUNDA_EVENT)
                                 .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                                 .status(BusinessProcessStatus.STARTED)
                                 .activityId(ACTIVITY_ID)
                                 .build())
            .ccdState(state)
            .build();
    }

    private CaseData getCase(List<Element<GASolicitorDetailsGAspec>> respondentSols,
                             List<Element<GARespondentResponse>> respondentsResponses) {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        DynamicList dynamicListTest = fromList(getSampleCourLocations());
        Optional<DynamicListElement> first = dynamicListTest.getListItems().stream().findFirst();
        first.ifPresent(dynamicListTest::setValue);

        return CaseData.builder()
            .generalAppRespondentSolicitors(respondentSols)
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        dynamicListTest)
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .build())
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
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .build();
    }

    private CaseData getCaseWithJudicialDecision(List<Element<GASolicitorDetailsGAspec>> respondentSols,
                             List<Element<GARespondentResponse>> respondentsResponses) {
        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        DynamicList dynamicListTest = fromList(getSampleCourLocations());
        Optional<DynamicListElement> first = dynamicListTest.getListItems().stream().findFirst();
        first.ifPresent(dynamicListTest::setValue);

        return CaseData.builder()
            .generalAppRespondentSolicitors(respondentSols)
            .hearingDetailsResp(GAHearingDetails.builder()
                                    .hearingPreferredLocation(
                                        dynamicListTest)
                                    .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .build())
            .respondentsResponses(respondentsResponses)
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .generalAppType(
                GAApplicationType
                    .builder()
                    .types(types).build())
            .judicialDecision(GAJudicialDecision.builder().decision(
                GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS).build())
            .businessProcess(BusinessProcess
                                 .builder()
                                 .camundaEvent(CAMUNDA_EVENT)
                                 .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                                 .status(BusinessProcessStatus.STARTED)
                                 .activityId(ACTIVITY_ID)
                                 .build())
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .build();
    }

    private DynamicList getLocationDynamicList(CaseData responseCaseData) {
        return responseCaseData.getHearingDetailsResp().getHearingPreferredLocation();
    }

    private List<String> locationsFromDynamicList(DynamicList dynamicList) {
        return dynamicList.getListItems().stream()
            .map(DynamicListElement::getLabel)
            .collect(Collectors.toList());
    }

    protected List<String> getSampleCourLocations() {
        return new ArrayList<>(Arrays.asList("ABCD - RG0 0AL", "PQRS - GU0 0EE", "WXYZ - EW0 0HE", "LMNO - NE0 0BH"));
    }
}
