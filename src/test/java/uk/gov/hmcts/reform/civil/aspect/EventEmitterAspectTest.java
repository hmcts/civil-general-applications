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
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;

import java.util.List;

import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    EventEmitterAspect.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class EventEmitterAspectTest {

    @Autowired
    EventEmitterAspect aspect;

    @Autowired
    CaseDetailsConverter caseDetailsConverter;

    @MockBean
    ProceedingJoinPoint proceedingJoinPoint;

    @MockBean
    EventEmitterService eventEmitterService;

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(value = CallbackType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SUBMITTED"})
    void shouldNotEmitBusinessProcessCamundaEvent_whenCallbackIsNotSubmitted(CallbackType callbackType) {
        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .type(callbackType)
            .build();

        aspect.emitBusinessProcessEvent(proceedingJoinPoint, callbackParams);

        verifyNoInteractions(eventEmitterService);
        verify(proceedingJoinPoint).proceed();
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(value = BusinessProcessStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"READY"})
    void shouldNotEmitBusinessProcessCamundaEvent_whenBusinessProcessStatusIsNotReadyAndProcessInstanceIdnull(BusinessProcessStatus status) {
        GeneralApplication generalApplication = GeneralApplication.builder()
            .businessProcess(BusinessProcess.builder().status(status).build())
            .build();
        List<Element<GeneralApplication>> newApplication = newArrayList();
        newApplication.add(element(generalApplication));
        CaseData caseData = CaseData.builder()
            .generalApplications(newApplication)
            .build();
        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .of(SUBMITTED, caseData)
            .build();

        aspect.emitBusinessProcessEvent(proceedingJoinPoint, callbackParams);

        verifyNoInteractions(eventEmitterService);
        verify(proceedingJoinPoint).proceed();
    }

    @SneakyThrows
    @Test
    void shouldEmitBusinessProcessCamundaEvent_whenCallbackIsSubmittedABusinessProcessStatusIsReadyAndProcessInstatntIsNotNull() {
        GeneralApplication generalApplication = GeneralApplication.builder()
            .businessProcess(BusinessProcess.ready(INITIATE_GENERAL_APPLICATION))
            .build();
        List<Element<GeneralApplication>> newApplication = newArrayList();
        newApplication.add(element(generalApplication));
        CaseData caseData = CaseData.builder()
            .generalApplications(newApplication)
            .ccdCaseReference(1L)
            .build();
        CallbackParams callbackParams = CallbackParamsBuilder.builder()
            .of(SUBMITTED, caseData)
            .build();
        Long caseId = 1L;
        aspect.emitBusinessProcessEvent(proceedingJoinPoint, callbackParams);

        verify(eventEmitterService).emitBusinessProcessCamundaEvent(caseId, generalApplication, false);
        verify(proceedingJoinPoint).proceed();
    }
}
