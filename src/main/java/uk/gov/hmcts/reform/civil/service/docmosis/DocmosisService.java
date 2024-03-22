package uk.gov.hmcts.reform.civil.service.docmosis;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;

import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class DocmosisService {

    private final GeneralAppLocationRefDataService generalAppLocationRefDataService;

    public LocationRefData getCaseManagementLocationVenueName(CaseData caseData, String authorisation) {
        List<LocationRefData> courtLocations = generalAppLocationRefDataService
            .getCourtLocations(authorisation);
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
