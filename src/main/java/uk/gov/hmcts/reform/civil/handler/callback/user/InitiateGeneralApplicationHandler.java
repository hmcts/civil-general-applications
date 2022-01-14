package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.*;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

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
    private final ObjectMapper objectMapper;

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

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        String body = buildConfirmationSummary(caseData);

        return SubmittedCallbackResponse.builder()
            .confirmationHeader("# You have made an application")
            .confirmationBody(body)
            .build();
    }

    @SuppressWarnings("checkstyle:LineLength")
    private String buildConfirmationSummary(CaseData caseData) {
        // Below is the mocked up data... we will get this information (like type(s) of application, multiparty or not,
        // notified or not etc.) from the collection itself from the caseData.
        // Until that object is ready, let's use a dummy one.
        List<String> applicationTypes = Arrays.asList("Strike Out", "Summary Judgement", "Extend Time");
        String collect = applicationTypes.stream().map(appType -> "<li>" + appType + "</li>")
            .collect(Collectors.joining());
        //TODO: Fix me
        boolean isApplicationUrgent = Optional.of(isEmpty(caseData.getGeneralApplications())).orElse(true);
        boolean isMultiParty = true;
        //TODO: Fix me
        boolean isNotified = Optional.of(caseData.getGeneralAppRespondentAgreement() == null).orElse(true);
        String lastLine = format(PARTY_NOTIFIED, isMultiParty ? "parties'" : "party's",
                                 isNotified ? "has been notified" : "has not been notified");
        return format(CONFIRMATION_SUMMARY,
                      applicationTypes.size() == 1 ? "this application" : "these applications",
                      collect,
                      isApplicationUrgent ? URGENT_APPLICATION : " ",
                      lastLine
        );
    }

    /**
     * To be used to return empty callback response, will be used in overriding classes.
     *
     * @param callbackParams This parameter is required as this is passed as reference for execute method in CallBack
     * @return empty callback response
     */
    protected CallbackResponse emptyCallbackResponse(CallbackParams callbackParams) {
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

    public List<Element<GeneralApplication>> addApplication(GeneralApplication application,
                                                            List<Element<GeneralApplication>>
                                                                generalApplicationDetails) {
        List<Element<GeneralApplication>> newApplication = ofNullable(generalApplicationDetails).orElse(newArrayList());
        newApplication.add(element(application));

        return newApplication;
    }
}
