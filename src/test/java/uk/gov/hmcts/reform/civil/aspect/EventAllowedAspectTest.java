package uk.gov.hmcts.reform.civil.aspect;

import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowStateAllowedEventService;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    EventAllowedAspect.class,
    FlowStateAllowedEventService.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StateFlowEngine.class})
class EventAllowedAspectTest {

    private static final String ERROR_MESSAGE = "This action cannot currently be performed because it has either "
        + "already been completed or another action must be completed first.";

    @Autowired
    EventAllowedAspect eventAllowedAspect;
    @MockBean
    ProceedingJoinPoint proceedingJoinPoint;
    @MockBean
    FeatureToggleService featureToggleService;

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"ABOUT_TO_START"})
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenCallbackTypeIsNotAboutToStart(CallbackType callbackType) {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .of(callbackType, CaseDetailsBuilder.builder().build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @SneakyThrows
    void shouldNotProceedToMethodInvocation_whenEventIsNotAllowed() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder()
            .errors(List.of(ERROR_MESSAGE))
            .build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_START)
            .request(CallbackRequest.builder()
                         .eventId(CaseEvent.MAKE_PAYMENT_SERVICE_REQ_GASPEC.name())
                         .caseDetails(CaseDetailsBuilder.builder().atStateAwaitingRespondentAcknowledgement().build())
                         .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint, never()).proceed();
    }

    @Test
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenEventIsAllowed() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_SUBMIT)
            .request(CallbackRequest.builder()
                .eventId(CaseEvent.INITIATE_GENERAL_APPLICATION.name())
                .caseDetails(CaseDetailsBuilder.builder().build())
                .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenEventIsAllowedForAboutToStartRespondToApplication() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_START)
            .request(CallbackRequest.builder()
                         .eventId(CaseEvent.RESPOND_TO_APPLICATION.name())
                         .caseDetails(CaseDetailsBuilder.builder()
                                          .data(CaseData.builder()
                                                    .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                                                    .isWithNotice(YES).build())
                                                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                                                       .hasAgreed(YES).build())
                                                    .generalAppPBADetails(GAPbaDetails.builder().paymentDetails(
                                                        PaymentDetails.builder().status(PaymentStatus.SUCCESS)
                                                            .build()).build())
                                                    .ccdCaseReference(12312312L)
                                                    .build()).build())
                         .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenEventIsAllowedForAboutToStartJudicialDecision() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_START)
            .request(CallbackRequest.builder()
                         .eventId(CaseEvent.MAKE_DECISION.name())
                         .caseDetails(CaseDetailsBuilder.builder()
                                          .data(CaseData.builder()
                                                    .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                                                    .isWithNotice(YES).build())
                                                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                                                       .hasAgreed(YES).build())
                                                    .generalAppPBADetails(GAPbaDetails.builder().paymentDetails(
                                                        PaymentDetails.builder().status(PaymentStatus.SUCCESS)
                                                            .build()).build())
                                                    .judicialDecision(GAJudicialDecision.builder().build())
                                                    .ccdCaseReference(12312312L)
                                                    .build()).build())
                         .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenEventIsAllowedForAboutToStartAdditionalInfo() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_START)
            .request(CallbackRequest.builder()
                         .eventId(CaseEvent.RESPOND_TO_JUDGE_ADDITIONAL_INFO.name())
                         .caseDetails(CaseDetailsBuilder.builder()
                                          .data(CaseData.builder()
                                                    .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                                                    .isWithNotice(YES).build())
                                                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                                                       .hasAgreed(YES).build())
                                                    .generalAppPBADetails(GAPbaDetails.builder().paymentDetails(
                                                        PaymentDetails.builder().status(PaymentStatus.SUCCESS)
                                                            .build()).build())
                                                    .judicialDecision(GAJudicialDecision.builder().decision(
                                                        GAJudgeDecisionOption.REQUEST_MORE_INFO).build())
                                                    .ccdCaseReference(32312312L)
                                                    .build()).build())
                         .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenEventIsAllowedForAboutToStartRespondDirection() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_START)
            .request(CallbackRequest.builder()
                         .eventId(CaseEvent.RESPOND_TO_JUDGE_DIRECTIONS.name())
                         .caseDetails(CaseDetailsBuilder.builder()
                                          .data(CaseData.builder()
                                                    .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                                                    .isWithNotice(YES).build())
                                                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                                                       .hasAgreed(YES).build())
                                                    .generalAppPBADetails(GAPbaDetails.builder().paymentDetails(
                                                        PaymentDetails.builder().status(PaymentStatus.SUCCESS)
                                                            .build()).build())
                                                    .judicialDecision(GAJudicialDecision.builder().decision(
                                                        GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                                                    .judicialDecisionMakeOrder(
                                                        GAJudicialMakeAnOrder.builder().makeAnOrder(
                                                        GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING)
                                                            .build())
                                                    .ccdCaseReference(32312312L)
                                                    .build()).build())
                         .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenEventIsAllowedForAboutToStartRespondWrittenRep() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_START)
            .request(CallbackRequest.builder()
                         .eventId(CaseEvent.RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION.name())
                         .caseDetails(CaseDetailsBuilder.builder()
                                          .data(CaseData.builder()
                                                    .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                                                    .isWithNotice(YES).build())
                                                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                                                       .hasAgreed(YES).build())
                                                    .generalAppPBADetails(GAPbaDetails.builder().paymentDetails(
                                                        PaymentDetails.builder().status(PaymentStatus.SUCCESS)
                                                            .build()).build())
                                                    .judicialDecision(GAJudicialDecision.builder().decision(
                                                        GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS)
                                                                          .build())
                                                    .ccdCaseReference(32312312L)
                                                    .build()).build())
                         .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @SneakyThrows
    void shouldProceedToMethodInvocation_whenEventIsAllowedForAboutToStartHearingScheduled() {
        AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
        when(proceedingJoinPoint.proceed()).thenReturn(response);

        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(ABOUT_TO_START)
            .request(CallbackRequest.builder()
                         .eventId(CaseEvent.HEARING_SCHEDULED_GA.name())
                         .caseDetails(CaseDetailsBuilder.builder()
                                          .data(CaseData.builder()
                                                    .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                                                                .judgeOtherSupport("support").build())
                                                    .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                                                    .isWithNotice(YES).build())
                                                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                                                       .hasAgreed(YES).build())
                                                    .generalAppPBADetails(GAPbaDetails.builder().paymentDetails(
                                                        PaymentDetails.builder().status(PaymentStatus.SUCCESS)
                                                            .build()).build())
                                                    .judicialDecision(GAJudicialDecision.builder().decision(
                                                        GAJudgeDecisionOption.LIST_FOR_A_HEARING).build())
                                                    .ccdCaseReference(32312312L)
                                                    .build()).build())
                         .build())
            .build();
        Object result = eventAllowedAspect.checkEventAllowed(proceedingJoinPoint, callbackParams);

        assertThat(result).isEqualTo(response);
        verify(proceedingJoinPoint).proceed();
    }

}
