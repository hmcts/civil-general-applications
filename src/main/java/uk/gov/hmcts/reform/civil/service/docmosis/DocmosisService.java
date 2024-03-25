package uk.gov.hmcts.reform.civil.service.docmosis;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class DocmosisService {

    private final IdamClient idamInfo;
    private final GeneralAppLocationRefDataService generalAppLocationRefDataService;
    private final List<LocationRefData> courtLocations;

    public List<LocationRefData> getCourtLocations(String authorisation) {
        if (courtLocations.isEmpty()) {
            return generalAppLocationRefDataService.getCourtLocations(authorisation);
        }
        return courtLocations;
    }

    public String getJudgeNameTitle(String authorisation) {
        UserDetails userDetails = idamInfo.getUserDetails(authorisation);
        return userDetails.getFullName();
    }

    public LocationRefData getCaseManagementLocationVenueName(CaseData caseData, String authorisation) {
        List<LocationRefData> courtLocations = getCourtLocations(authorisation);
        var matchingLocations =
            courtLocations
                .stream()
                .filter(location -> location.getEpimmsId()
                    .equals(caseData.getCaseManagementLocation().getBaseLocation())).toList();

        if (!matchingLocations.isEmpty()) {
            return matchingLocations.get(0);
        } else {
            throw new IllegalArgumentException("Court Name is not found in location data");
        }
    }

    public String populateJudicialHearingLocationVenueName(CaseData caseData, String authorisation) {
        List<LocationRefData> hearingLocations = getCourtLocations(authorisation);
        if (Objects.nonNull(caseData.getJudicialListForHearing().getHearingPreferencesPreferredType())
            && caseData.getJudicialListForHearing().getHearingPreferencesPreferredType()
            .equals(GAJudicialHearingType.IN_PERSON)) {

            String judicialHearingLocationPostCode = getJudicialHearingLocationPostCode(caseData);

            var matchingLocation =
                hearingLocations
                    .stream()
                    .filter(location -> location.getPostcode()
                        .equals(judicialHearingLocationPostCode)).findAny();

            if (!matchingLocation.isEmpty()) {
                return matchingLocation.get().getVenueName();
            }
        }
        throw new IllegalArgumentException("Venue Name is not found in location data");

    }

    private String getJudicialHearingLocationPostCode(CaseData caseData) {

        String hearingLocation = caseData.getJudicialListForHearing().getHearingPreferredLocation().getValue()
            .getLabel();

        return hearingLocation.substring(hearingLocation.lastIndexOf("-") + 2);

    }

    public YesOrNo reasonAvailable(CaseData caseData) {
        if (Objects.nonNull(caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision())
            && caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision().equals(YesOrNo.NO)) {
            return YesOrNo.NO;
        }
        return YesOrNo.YES;
    }

    public String populateJudgeReason(CaseData caseData) {
        if (Objects.nonNull(caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision())
            && caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision().equals(YesOrNo.NO)) {
            return "";
        }
        return caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText() != null
            ? caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText()
            : "";
    }

    public String populateJudicialByCourtsInitiative(CaseData caseData) {

        if (caseData.getJudicialDecisionMakeOrder().getJudicialByCourtsInitiative().equals(GAByCourtsInitiativeGAspec
                                                                                               .OPTION_3)) {
            return StringUtils.EMPTY;
        }

        if (caseData.getJudicialDecisionMakeOrder().getJudicialByCourtsInitiative()
            .equals(GAByCourtsInitiativeGAspec.OPTION_1)) {
            return caseData.getJudicialDecisionMakeOrder().getOrderCourtOwnInitiative() + " "
                .concat(caseData.getJudicialDecisionMakeOrder().getOrderCourtOwnInitiativeDate()
                            .format(DATE_FORMATTER));
        } else {
            return caseData.getJudicialDecisionMakeOrder().getOrderWithoutNotice() + " "
                .concat(caseData.getJudicialDecisionMakeOrder().getOrderWithoutNoticeDate()
                            .format(DATE_FORMATTER));
        }
    }
}
