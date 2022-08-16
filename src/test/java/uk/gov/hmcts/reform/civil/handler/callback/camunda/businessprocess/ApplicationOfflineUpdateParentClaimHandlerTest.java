package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_OFFLINE_UPDATE_PARENT_CLAIM;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_PROCEEDS_IN_HERITAGE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    ApplicationOfflineUpdateParentClaimHandler.class, ParentCaseUpdateHelper.class
})
class ApplicationOfflineUpdateParentClaimHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @Autowired
    private ApplicationOfflineUpdateParentClaimHandler handler;

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldUpdateParentClaimWhenValidCaseData() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(PROCEEDS_IN_HERITAGE)
                .businessProcess(BusinessProcess.ready(APPLICATION_PROCEEDS_IN_HERITAGE))
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            handler.handle(params);

            verify(parentCaseUpdateHelper, times(1)).updateParentWithGAState(
                caseData,
                PROCEEDS_IN_HERITAGE.getDisplayedValue()
            );
        }

        @Test
        void shouldNotUpdateParentClaimWhenBusinessProcessIsAvailable() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            handler.handle(params);

            verifyNoInteractions(parentCaseUpdateHelper);
        }

        @Test
        void shouldNotUpdateParentClaimWhenBusinessProcessIsNotMainCaseClosed() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .businessProcess(BusinessProcess.ready(INITIATE_GENERAL_APPLICATION))
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            handler.handle(params);

            verifyNoInteractions(parentCaseUpdateHelper);
        }

        @ParameterizedTest(name = "The application is in {0} state")
        @EnumSource(
            value = CaseState.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"PROCEEDS_IN_HERITAGE"})
        void shouldNotUpdateParentClaimWhenCaseStateIsNotApplicationClosed(CaseState state) {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(state)
                .businessProcess(BusinessProcess.ready(APPLICATION_PROCEEDS_IN_HERITAGE))
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            handler.handle(params);

            verifyNoInteractions(parentCaseUpdateHelper);
        }
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(APPLICATION_OFFLINE_UPDATE_PARENT_CLAIM);
    }
}
