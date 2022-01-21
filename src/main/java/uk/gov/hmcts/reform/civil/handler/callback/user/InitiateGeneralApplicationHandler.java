package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;

@SuppressWarnings({"checkstyle:Indentation", "checkstyle:EmptyLineSeparator"})
@Service
@RequiredArgsConstructor
public class InitiateGeneralApplicationHandler extends CallbackHandler {

    private static final String CONFIRMATION_SUMMARY = "<br/><p> Your Court will make a decision on %s."
        + "<ul> %s </ul>"
        + "</p> %s"
        + " %s ";
    private static final String URGENT_APPLICATION = "<p> You have marked this application as urgent. </p>";
    private static final String PARTY_NOTIFIED = "<p> The other %s legal representative %s "
        + "that you have submitted this application.";
    private static final List<CaseEvent> EVENTS = Collections.singletonList(INITIATE_GENERAL_APPLICATION);

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
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        String body = null;
        if (generalApplications != null) {
            Optional<Element<GeneralApplication>> generalApplicationElementOptional = generalApplications.stream()
                .filter(app -> app.getValue() != null && app.getValue().getBusinessProcess() != null
                    && app.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.READY
                    && app.getValue().getBusinessProcess().getProcessInstanceId() == null).findFirst();
            if (generalApplicationElementOptional.isPresent()) {
                GeneralApplication generalApplicationElement = generalApplicationElementOptional.get().getValue();
                body = buildConfirmationSummary(generalApplicationElement);
            }
        }

        return SubmittedCallbackResponse.builder()
            .confirmationHeader("# You have made an application")
            .confirmationBody(body)
            .build();
    }

    private String buildConfirmationSummary(GeneralApplication application) {
        List<GeneralApplicationTypes> types = application.getGeneralAppType().getTypes();
        String collect = types.stream().map(appType -> "<li>" + appType.getDisplayedValue() + "</li>")
            .collect(Collectors.joining());
        boolean isApplicationUrgent = Optional.of(application.getGeneralAppUrgencyRequirement().getGeneralAppUrgency()
                                                      == YesOrNo.YES).orElse(true);
        boolean isMultiParty = Optional.of(application.getIsMultiParty() == YesOrNo.YES).orElse(true);
        boolean isNotified = Optional.of(application.getGeneralAppInformOtherParty().getIsWithNotice()
                                             == YesOrNo.YES).orElse(true);
        String lastLine = format(PARTY_NOTIFIED, isMultiParty ? "parties'" : "party's",
                                 isNotified ? "has been notified" : "has not been notified"
        );
        return format(
            CONFIRMATION_SUMMARY,
            types.size() == 1 ? "this application" : "these applications",
            collect,
            isApplicationUrgent ? URGENT_APPLICATION : " ",
            lastLine
        );
    }
}
