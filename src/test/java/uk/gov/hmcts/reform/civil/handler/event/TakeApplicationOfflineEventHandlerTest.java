package uk.gov.hmcts.reform.civil.handler.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.event.TakeApplicationOfflineEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_PROCEEDS_IN_HERITAGE;

@ExtendWith(SpringExtension.class)

class TakeApplicationOfflineEventHandlerTest {

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @Mock
    private CaseDetailsConverter caseDetailsConverter;

    @InjectMocks
    private TakeApplicationOfflineEventHandler handler;

    @Test
    void shouldTriggerMainCaseClosedEventForGeneralApplication() {
        CaseData caseData = CaseData.builder().ccdCaseReference(1L).build();
        TakeApplicationOfflineEvent event = new TakeApplicationOfflineEvent(1L);
        when(coreCaseDataService.getCase(event.getCaseId()))
            .thenReturn(CaseDetailsBuilder.builder().data(caseData).build());
        when(caseDetailsConverter.toCaseData(any(CaseDetails.class)))
            .thenReturn(caseData);
        handler.triggerApplicationProceedsInHeritageEvent(event);

        verify(coreCaseDataService, times(1))
            .triggerGaEvent(eq(1L), eq(APPLICATION_PROCEEDS_IN_HERITAGE), any());
    }
}

