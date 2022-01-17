package uk.gov.hmcts.reform.civil.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;

import java.util.List;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class EventEmitterAspect {

    private final EventEmitterService eventEmitterService;

    @SuppressWarnings({"checkstyle:WhitespaceAround", "checkstyle:LineLength"})
    @Around("execution(* *(*)) && @annotation(EventEmitter) && args(callbackParams))")
    public Object emitBusinessProcessEvent(ProceedingJoinPoint joinPoint, CallbackParams callbackParams)
        throws Throwable {
        if (callbackParams.getType() == SUBMITTED) {
            CaseData caseData = callbackParams.getCaseData();
            var caseId = caseData.getCcdCaseReference();
            List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
            generalApplications.forEach(app -> {
                if (BusinessProcessStatus.READY == app.getValue().getBusinessProcess().getStatus()) {
                    eventEmitterService.emitBusinessProcessCamundaEvent(caseId, app.getValue(), false);
                }
            });
        }
        return joinPoint.proceed();
    }
}
