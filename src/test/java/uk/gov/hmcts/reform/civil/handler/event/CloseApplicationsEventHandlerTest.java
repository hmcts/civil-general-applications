package uk.gov.hmcts.reform.civil.handler.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.event.CloseApplicationsEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAIN_CASE_CLOSED;

@ExtendWith(SpringExtension.class)
class CloseApplicationsEventHandlerTest {

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private CaseDetailsConverter caseDetailsConverter;

    @InjectMocks
    private CloseApplicationsEventHandler handler;

    @Test
    void shouldTriggerMainCaseClosedEventForGeneralApplication() {
        CaseData caseData = CaseData.builder().ccdCaseReference(1L).build();
        CloseApplicationsEvent event = new CloseApplicationsEvent(1L);
        when(coreCaseDataService.getCase(event.getCaseId()))
            .thenReturn(CaseDetailsBuilder.builder().data(caseData).build());
        when(caseDetailsConverter.toCaseData(any(CaseDetails.class)))
            .thenReturn(caseData);
        handler.triggerApplicationClosedEvent(event);

        verify(coreCaseDataService, times(1)).triggerGaEvent(eq(1L), eq(MAIN_CASE_CLOSED),
                                                           any());
    }
}
