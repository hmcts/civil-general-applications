package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
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
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
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

    @SuppressWarnings("checkstyle:CommentsIndentation")
    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
            callbackKey(ABOUT_TO_SUBMIT), this::emptyCallbackResponse,
            callbackKey(SUBMITTED), this::buildConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
    @SuppressWarnings("checkstyle:RightCurly")
    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        final GeneralApplication[] gApp = {null};
        String body = "";
        generalApplications.forEach(app -> {
            if (BusinessProcessStatus.READY == app.getValue().getBusinessProcess().getStatus()) {
                gApp[0] = app.getValue();        }
        });
        if (gApp[0] != null) {
            body = buildConfirmationSummary(gApp[0]);
        }
        return SubmittedCallbackResponse.builder()
            .confirmationHeader("# You have made an application")
            .confirmationBody(body)
            .build();
    }

    @SuppressWarnings("checkstyle:LineLength")
    private String buildConfirmationSummary(GeneralApplication application) {
        List<GeneralApplicationTypes> types = application.getGeneralAppType().getTypes();
        String collect = types.stream().map(appType -> "<li>" + appType + "</li>")
            .collect(Collectors.joining());
        boolean isApplicationUrgent = Optional.of(application.getGeneralAppUrgencyRequirement().getGeneralAppUrgency()
                                                      == YesOrNo.YES).orElse(true);
        boolean isMultiParty = Optional.of(application.getIsMultiParty() == YesOrNo.YES).orElse(true);
        boolean isNotified = Optional.of(application.getGeneralAppInformOtherParty().getIsWithNotice() == YesOrNo.YES).orElse(
            true);
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
    /*
     * To be used to return empty callback response, will be used in overriding classes.
     *
     * @param callbackParams This parameter is required as this is passed as reference for execute method in CallBack
     * @return empty callback response
     */
    protected CallbackResponse emptyCallbackResponse(CallbackParams callbackParams) {
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }
}
