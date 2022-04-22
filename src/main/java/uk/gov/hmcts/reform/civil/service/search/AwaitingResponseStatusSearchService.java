package uk.gov.hmcts.reform.civil.service.search;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@Service
public class AwaitingResponseStatusSearchService extends ElasticSearchService {

    private final CaseDetailsConverter caseDetailsConverter;

    public AwaitingResponseStatusSearchService(CoreCaseDataService coreCaseDataService,
                                               CaseDetailsConverter caseDetailsConverter) {
        super(coreCaseDataService);
        this.caseDetailsConverter = caseDetailsConverter;
    }

    public Query query(int startIndex) {

        return new Query(
            matchQuery("state", AWAITING_RESPONDENT_RESPONSE.toString()),
            emptyList(),
            startIndex
        );
    }

    public List<CaseDetails> getAwaitingResponseCasesThatArePastDueDate() {
        List<CaseDetails> awaitingResponseCases = getGeneralApplications();
        return awaitingResponseCases.stream()
            .filter(a -> caseDetailsConverter.toCaseData(a).getGeneralAppDeadlineNotificationDate() != null)
            .filter(a -> LocalDateTime.now().isAfter(
                LocalDateTime.parse(
                    caseDetailsConverter.toCaseData(a).getGeneralAppDeadlineNotificationDate())))
            .collect(Collectors.toList());
    }
}
