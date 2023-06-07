package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;

@SuppressWarnings({"checkstyle:Indentation", "checkstyle:EmptyLineSeparator"})
@Service
@RequiredArgsConstructor
public class InitiateGeneralApplicationHandler extends CallbackHandler {

    private static final String CONFIRMATION_BODY = "<br/> <p> Your application fee of £%s"
        + " is now due for payment. Your application will not be reviewed by the"
        + " court until this fee has been paid."
        + "%n%n To pay this fee, you will need to open your application from the"
        + " Applications tab of this case listing."
        + "%n%n <a href=\"%s\" target=\"_blank\">Pay your application fee </a> %n";

    private static final String CONFIRMATION_BODY_FREE = "<br/> <p> The court will make a decision"
            + " on this application."
            + "<br/> <p>  The other party's legal representative has been notified that you have"
            + " submitted this application";

    private static final List<CaseEvent> EVENTS = Collections.singletonList(INITIATE_GENERAL_APPLICATION);
    private final GeneralAppFeesService generalAppFeesService;
    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(SUBMITTED), this::buildConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        Long ccdCaseReference = caseData.getCcdCaseReference();
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        String body = null;
        if (generalApplications != null) {
            Optional<Element<GeneralApplication>> generalApplicationElementOptional = generalApplications.stream()
                .filter(app -> app.getValue() != null && app.getValue().getBusinessProcess() != null
                    && app.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.READY
                    && app.getValue().getBusinessProcess().getProcessInstanceId() == null).findFirst();
            if (generalApplicationElementOptional.isPresent()) {
                GeneralApplication generalApplicationElement = generalApplicationElementOptional.get().getValue();
                body = buildConfirmationSummary(generalApplicationElement, ccdCaseReference);
            }
        }

        return SubmittedCallbackResponse.builder()
            .confirmationHeader("# You have made an application")
            .confirmationBody(body)
            .build();
    }

    private String buildConfirmationSummary(GeneralApplication application, Long ccdCaseReference) {

        BigDecimal fee = application.getGeneralAppPBADetails().getFee().toPounds();
        return generalAppFeesService.isFreeGa(application) ? CONFIRMATION_BODY_FREE : format(
            CONFIRMATION_BODY,
            fee,
            format("/cases/case-details/%s#Applications", ccdCaseReference)
        );
    }

}
