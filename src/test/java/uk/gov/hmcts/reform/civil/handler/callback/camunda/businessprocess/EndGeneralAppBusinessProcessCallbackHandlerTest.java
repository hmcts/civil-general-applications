package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAStatementOfTruth;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_BUSINESS_PROCESS_GASPEC;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.RELIEF_FROM_SANCTIONS;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SpringBootTest(classes = {
    EndGeneralAppBusinessProcessCallbackHandler.class,
    CaseDetailsConverter.class,
    CoreCaseDataService.class,
    ParentCaseUpdateHelper.class,
    ObjectMapper.class,
    ApplicationNotificationUtil.class
})
public class EndGeneralAppBusinessProcessCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private EndGeneralAppBusinessProcessCallbackHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    private static final String STRING_CONSTANT = "STRING_CONSTANT";
    private static final Long CHILD_CCD_REF = 1646003133062762L;
    private static final Long PARENT_CCD_REF = 1645779506193000L;

    @Nested
    class AboutToSubmitCallback {
        private final ArgumentCaptor<String> parentCaseId = ArgumentCaptor.forClass(String.class);
        private final ArgumentCaptor<CaseDataContent> caseDataContent = ArgumentCaptor.forClass(CaseDataContent.class);

        @Test
        void theEndOfProcessShouldUpdateTheStateOfGAAndAlsoUpdateStateOnParentCaseGADetails_NotToBeNotified() {
            when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
            when(coreCaseDataService.caseDataContentFromStartEventResponse(any(), anyMap())).thenCallRealMethod();
            when(caseDetailsConverter.toCaseData(getCallbackParams(YES, NO).getRequest().getCaseDetails()))
                    .thenReturn(getSampleGeneralApplicationCaseData(YES, NO));
            when(caseDetailsConverter.toCaseData(getStartEventResponse(YES, NO).getCaseDetails()))
                    .thenReturn(getParentCaseDataBeforeUpdate(YES, NO));

            handler.handle(getCallbackParams(YES, NO));

            verify(coreCaseDataService, times(1))
                    .startUpdate("1645779506193000", UPDATE_CASE_WITH_GA_STATE);

            verify(coreCaseDataService).submitUpdate(parentCaseId.capture(), caseDataContent.capture());
            HashMap<?, ?> updatedCaseData = (HashMap<?, ?>) caseDataContent.getValue().getData();

            List<?> generalApplications = objectMapper.convertValue(updatedCaseData.get("generalApplications"),
                    new TypeReference<>(){});
            List<?> generalApplicationDetails = objectMapper.convertValue(
                    updatedCaseData.get("generalApplicationsDetails"), new TypeReference<>(){});
            assertThat(generalApplications.size()).isEqualTo(1);
            assertThat(generalApplicationDetails.size()).isEqualTo(1);

            GeneralApplicationsDetails generalApp = objectMapper.convertValue(
                    ((LinkedHashMap<?, ?>) generalApplicationDetails.get(0)).get("value"),
                    new TypeReference<>() {});
            assertThat(generalApp.getCaseState()).isEqualTo("Application Submitted - Awaiting Judicial Decision");
        }

        @Test
        void theEndOfProcessShouldUpdateTheStateOfGAAndAlsoUpdateStateOnParentCaseGADetails_ToBeNotified() {
            when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(NO, YES));
            when(coreCaseDataService.caseDataContentFromStartEventResponse(any(), anyMap())).thenCallRealMethod();
            when(caseDetailsConverter.toCaseData(getCallbackParams(NO, YES).getRequest().getCaseDetails()))
                    .thenReturn(getSampleGeneralApplicationCaseData(NO, YES));
            when(caseDetailsConverter.toCaseData(getStartEventResponse(NO, YES).getCaseDetails()))
                    .thenReturn(getParentCaseDataBeforeUpdate(NO, YES));

            handler.handle(getCallbackParams(NO, YES));

            verify(coreCaseDataService, times(1))
                    .startUpdate("1645779506193000", UPDATE_CASE_WITH_GA_STATE);

            verify(coreCaseDataService).submitUpdate(parentCaseId.capture(), caseDataContent.capture());
            HashMap<?, ?> updatedCaseData = (HashMap<?, ?>) caseDataContent.getValue().getData();

            List<?> generalApplications = objectMapper.convertValue(updatedCaseData.get("generalApplications"),
                    new TypeReference<>(){});
            List<?> generalApplicationDetails = objectMapper.convertValue(
                    updatedCaseData.get("generalApplicationsDetails"), new TypeReference<>(){});
            assertThat(generalApplications.size()).isEqualTo(1);
            assertThat(generalApplicationDetails.size()).isEqualTo(1);

            GeneralApplicationsDetails generalApp = objectMapper.convertValue(
                    ((LinkedHashMap<?, ?>) generalApplicationDetails.get(0)).get("value"),
                    new TypeReference<>() {});
            assertThat(generalApp.getCaseState()).isEqualTo("Awaiting Respondent Response");
        }

        @Test
        void handleEventsReturnsTheExpectedCallbackEvent() {
            assertThat(handler.handledEvents()).contains(END_BUSINESS_PROCESS_GASPEC);
        }

        private GeneralApplication getGeneralApplication(YesOrNo isConsented, YesOrNo isTobeNotified) {
            return GeneralApplication.builder()
                    .generalAppType(GAApplicationType.builder().types(List.of(RELIEF_FROM_SANCTIONS)).build())
                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(isConsented).build())
                    .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isTobeNotified).build())
                    .generalAppPBADetails(GAPbaDetails.builder().build())
                    .generalAppDetailsOfOrder(STRING_CONSTANT)
                    .generalAppReasonsOfOrder(STRING_CONSTANT)
                    .respondentSolicitor1EmailAddress("respondent@email.com")
                    .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                    .generalAppStatementOfTruth(GAStatementOfTruth.builder().build())
                    .generalAppHearingDetails(GAHearingDetails.builder().build())
                    .generalAppRespondentSolicitors(wrapElements(GASolicitorDetailsGAspec.builder()
                            .email("abc@gmail.com").build()))
                    .isMultiParty(NO)
                    .parentClaimantIsApplicant(YES)
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                            .caseReference(PARENT_CCD_REF.toString()).build())
                    .build();
        }

        private CaseData getSampleGeneralApplicationCaseData(YesOrNo isConsented, YesOrNo isTobeNotified) {
            return CaseDataBuilder.builder().buildCaseDateBaseOnGeneralApplication(
                    getGeneralApplication(isConsented, isTobeNotified))
                    .toBuilder().ccdCaseReference(CHILD_CCD_REF).build();
        }

        private CallbackParams getCallbackParams(YesOrNo isConsented, YesOrNo isTobeNotified) {
            return CallbackParams.builder()
                    .type(ABOUT_TO_SUBMIT)
                    .pageId(null)
                    .request(CallbackRequest.builder()
                            .caseDetails(CaseDetails.builder()
                                    .data(objectMapper.convertValue(
                                            getSampleGeneralApplicationCaseData(isConsented, isTobeNotified),
                                            new TypeReference<Map<String, Object>>() {})).id(CASE_ID).build())
                            .eventId("END_BUSINESS_PROCESS_GASPEC")
                            .build())
                    .caseData(getSampleGeneralApplicationCaseData(isConsented, isTobeNotified))
                    .version(null)
                    .params(null)
                    .build();
        }

        private StartEventResponse getStartEventResponse(YesOrNo isConsented, YesOrNo isTobeNotified) {
            CaseDetails caseDetails = CaseDetailsBuilder.builder().data(
                    getParentCaseDataBeforeUpdate(isConsented, isTobeNotified))
                    .id(1645779506193000L)
                    .state(PENDING_CASE_ISSUED)
                    .build();
            StartEventResponse.StartEventResponseBuilder startEventResponseBuilder = StartEventResponse.builder();
            startEventResponseBuilder.eventId(UPDATE_CASE_WITH_GA_STATE.toString())
                    .token("BEARER_TOKEN")
                    .caseDetails(caseDetails);

            return startEventResponseBuilder.build();
        }

        private CaseData getParentCaseDataBeforeUpdate(YesOrNo isConsented, YesOrNo isTobeNotified) {
            return CaseData.builder()
                    .generalApplications(wrapElements(getGeneralApplication(isConsented, isTobeNotified)))
                    .generalApplicationsDetails(wrapElements(GeneralApplicationsDetails.builder()
                            .caseLink(CaseLink.builder().caseReference(CHILD_CCD_REF.toString()).build())
                            .caseState("General Application Issue Pending")
                            .build()))
                    .build();
        }
    }

}
