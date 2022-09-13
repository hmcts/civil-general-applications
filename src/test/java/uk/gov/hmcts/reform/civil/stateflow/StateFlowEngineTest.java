package uk.gov.hmcts.reform.civil.stateflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;
import uk.gov.hmcts.reform.civil.stateflow.model.State;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.DRAFT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PAYMENT_FAILED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PAYMENT_SUCCESSFUL;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StateFlowEngine.class
})

public class StateFlowEngineTest {

    @Autowired
    private StateFlowEngine stateFlowEngine;

    @MockBean
    private FeatureToggleService featureToggleService;

    @Test
    void shouldReturnPaymentSuccessfulWhenPBAPaymentIsSuccess() {
        CaseData caseData = CaseDataBuilder.builder().buildPaymentSuccessfulCaseData();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);
        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(PAYMENT_SUCCESSFUL.fullName());
        assertThat(stateFlow.getStateHistory()).hasSize(2)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), PAYMENT_SUCCESSFUL.fullName());
    }

    @Test
    void shouldReturnPaymentFailedWhenPBAPaymentIsFailed() {
        CaseData caseData = CaseDataBuilder.builder().buildPaymentFailureCaseData();

        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);
        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(PAYMENT_FAILED.fullName());
        assertThat(stateFlow.getStateHistory()).hasSize(2)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), PAYMENT_FAILED.fullName());

    }

}
