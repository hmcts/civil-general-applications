package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

import java.util.List;

@SpringBootTest(classes = {
    ParentCaseUpdateHelper.class,
    ObjectMapper.class,
})
class ParentCaseUpdateHelperTest {

    @Autowired
    ParentCaseUpdateHelper parentCaseUpdateHelper;
    @MockBean
    CoreCaseDataService coreCaseDataService;
    @MockBean
    CaseDetailsConverter caseDetailsConverter;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void updateParentApplicationVisibilityWithNewState() {
        CaseData caseData = CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication(NO)
            .submittedOn(null).build();

        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);

        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(caseData, ORDER_MADE.toString());
        verify(coreCaseDataService, times(1)).submitUpdate(any(), any());
    }

    @Test
    void updateParentApplicationVisibilityWithNewStateWithoutRespondentSol() {
        CaseData caseData = getCaseWithApplicationData(false);
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);

        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(caseData, ORDER_MADE.toString());
        verify(coreCaseDataService, times(1)).submitUpdate(any(), any());

    }

    @Test
    void updateParentApplicationVisibilityWithNewStateWithRespondentSol() {
        CaseData caseData = getCaseWithApplicationData(true);
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);

        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(caseData, ORDER_MADE.toString());
        verify(coreCaseDataService, times(1)).submitUpdate(any(), any());

    }
    private StartEventResponse getStartEventResponse(YesOrNo isConsented, YesOrNo isTobeNotified) {
        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(
            CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication(NO).build())
            .id(1645779506193000L)
            .state(APPLICATION_ADD_PAYMENT)
            .build();
        StartEventResponse.StartEventResponseBuilder startEventResponseBuilder = StartEventResponse.builder();
        startEventResponseBuilder.eventId(UPDATE_CASE_WITH_GA_STATE.toString())
            .token("BEARER_TOKEN")
            .caseDetails(caseDetails);

        return startEventResponseBuilder.build();
    }

    private CaseData getCaseWithApplicationData(Boolean withRespondentSol) {
        GeneralApplication generalApplication = GeneralApplication
                .builder()
                .caseLink(CaseLink.builder().caseReference(CaseDataBuilder.CASE_ID.toString()).build())
                .build();
        List<Element<GeneralApplication>> generalApplications = wrapElements(generalApplication);

        List<Element<GeneralApplicationsDetails>> generalApplicationsDetailsList = Lists.newArrayList();
        List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolList = Lists.newArrayList();

        GeneralApplicationsDetails generalApplicationsDetails = GeneralApplicationsDetails.builder()
                .generalApplicationType("Summary judgment")
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState("pending").build();
        generalApplicationsDetailsList.add(element(generalApplicationsDetails));

        GADetailsRespondentSol gaDetailsRespondentSol = GADetailsRespondentSol.builder()
                .generalApplicationType("Summary judgment")
                .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
                .caseLink(generalApplication.getCaseLink())
                .caseState("pending").build();
        gaDetailsRespondentSolList.add(element(gaDetailsRespondentSol));

        return CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication(NO)
                .generalApplications(generalApplications)
                .generalApplicationsDetails(generalApplicationsDetailsList)
                .gaDetailsRespondentSol(withRespondentSol?gaDetailsRespondentSolList:null)
                .submittedOn(null).build();
    }

}
