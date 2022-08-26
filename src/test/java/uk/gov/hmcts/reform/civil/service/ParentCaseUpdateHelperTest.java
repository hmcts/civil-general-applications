package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
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
        CaseData caseData = CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication().build();

        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(getStartEventResponse(YES, NO));
        when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);

        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(caseData, ORDER_MADE.toString());
        verify(coreCaseDataService, times(1)).submitUpdate(any(), any());
    }

    private StartEventResponse getStartEventResponse(YesOrNo isConsented, YesOrNo isTobeNotified) {
        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(
            CaseDataBuilder.builder().judicialOrderMadeWithUncloakApplication().build())
            .id(1645779506193000L)
            .state(APPLICATION_ADD_PAYMENT)
            .build();
        StartEventResponse.StartEventResponseBuilder startEventResponseBuilder = StartEventResponse.builder();
        startEventResponseBuilder.eventId(UPDATE_CASE_WITH_GA_STATE.toString())
            .token("BEARER_TOKEN")
            .caseDetails(caseDetails);

        return startEventResponseBuilder.build();
    }

}
