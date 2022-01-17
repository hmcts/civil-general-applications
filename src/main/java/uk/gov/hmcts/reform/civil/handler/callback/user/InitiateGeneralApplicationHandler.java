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
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
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
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        generalApplications.forEach(app -> {
            if (BusinessProcessStatus.READY == app.getValue().getBusinessProcess().getStatus()) {
                String body = buildConfirmationSummary(app.getValue());
            }
        });

        return SubmittedCallbackResponse.builder()
            .confirmationHeader("# You have made an application")
            .confirmationBody(null)
            .build();
    }

    @SuppressWarnings("checkstyle:LineLength")
    private String buildConfirmationSummary(GeneralApplication application) {
        List<String> applicationTypes = List.of(application.getGeneralAppType().toString());
        String collect = applicationTypes.stream().map(appType -> "<li>" + appType + "</li>")
            .collect(Collectors.joining());
        boolean isApplicationUrgent = Optional.of(application.getGeneralAppUrgencyRequirement().getGeneralAppUrgency()
                                                      == YesOrNo.NO).orElse(true);
        boolean isMultiParty = Optional.of(application.getIsMultiParty() == YesOrNo.NO).orElse(true);
        boolean isNotified = Optional.of(application.getGeneralAppRespondentAgreement().getHasAgreed() == YesOrNo.NO).orElse(
            true);
        String lastLine = format(PARTY_NOTIFIED, isMultiParty ? "parties'" : "party's",
                                 isNotified ? "has been notified" : "has not been notified"
        );
        return format(
            CONFIRMATION_SUMMARY,
            applicationTypes.size() == 1 ? "this application" : "these applications",
            collect,
            isApplicationUrgent ? URGENT_APPLICATION : " ",
            lastLine
        );
    }


    /*  List<String> applicationTypes = Arrays.asList("STRIKE_OUT");
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
*/
    /*
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
