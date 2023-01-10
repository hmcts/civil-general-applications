package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.HEARING_SCHEDULED;
import static uk.gov.hmcts.reform.civil.model.common.DynamicList.fromList;

@Service
@RequiredArgsConstructor
public class NotifyHearingHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(HEARING_SCHEDULED);

    private final GeneralAppLocationRefDataService locationRefDataService;

    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
                callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
                callbackKey(MID, "hearing-locations"), this::locationList,
                callbackKey(MID, "hearing-check-date"), this::checkFutureDate,
                callbackKey(ABOUT_TO_SUBMIT), this::nothing,
                callbackKey(SUBMITTED), this::nothing
        );
    }

    private CallbackResponse locationList(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        DynamicList dynamicLocationList = getLocationsFromList(locationRefDataService.getCourtLocations(authToken));
        if(Objects.nonNull(caseData.getJudicialListForHearing())
                && Objects.nonNull(caseData.getJudicialListForHearing().getHearingPreferredLocation())
                && Objects.nonNull(caseData.getJudicialListForHearing().getHearingPreferredLocation().getValue())
        ) {
            String preLabel = caseData.getJudicialListForHearing().getHearingPreferredLocation().getValue().getLabel();
            Optional<DynamicListElement> first = dynamicLocationList.getListItems().stream()
                    .filter(l -> l.getLabel().equals(preLabel)).findFirst();
            first.ifPresent(dynamicLocationList::setValue);
        }
        caseDataBuilder.hearingLocation(dynamicLocationList)
                .build();

        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private DynamicList getLocationsFromList(final List<LocationRefData> locations) {
        return fromList(locations.stream().map(location -> new StringBuilder().append(location.getSiteName())
                        .append(" - ").append(location.getCourtAddress())
                        .append(" - ").append(location.getPostcode()).toString())
                .collect(Collectors.toList()));
    }

    private CallbackResponse checkFutureDate(CallbackParams callbackParams) {
        List<String> errors = new ArrayList<>();
        LocalDateTime hearingDateTime = null;
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        LocalDate date = caseData.getHearingDate();
        String hourMinute = caseData.getHearingTimeHourMinute();
        if (hourMinute != null) {
            int hours = Integer.parseInt(hourMinute.substring(0, 2));
            int minutes = Integer.parseInt(hourMinute.substring(2, 4));
            LocalTime time = LocalTime.of(hours, minutes, 0);
            hearingDateTime = LocalDateTime.of(date, time);
        } else {
            errors.add("Time is required");
        }

        errors = (Objects.isNull(hearingDateTime)) ? null :
                isFutureDate(hearingDateTime);
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .errors(errors)
                .build();
    }

    private List<String> isFutureDate(LocalDateTime hearingDateTime) {
        List<String> errors = new ArrayList<>();
        if (!checkFutureDateValidation(hearingDateTime)) {
            errors.add("The Date & Time must be 24hs in advance from now");
        }
        return errors;
    }

    private boolean checkFutureDateValidation(LocalDateTime localDateTime) {
        return localDateTime != null && localDateTime.isAfter(LocalDateTime.now().plusHours(24));
    }

    private CallbackResponse nothing(CallbackParams callbackParams) {
        return AboutToStartOrSubmitCallbackResponse.builder()
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
