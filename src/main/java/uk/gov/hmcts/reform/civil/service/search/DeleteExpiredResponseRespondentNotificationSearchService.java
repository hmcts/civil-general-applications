package uk.gov.hmcts.reform.civil.service.search;

import java.util.ArrayList;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@Service
public class DeleteExpiredResponseRespondentNotificationSearchService extends ElasticSearchService {

    public DeleteExpiredResponseRespondentNotificationSearchService(CoreCaseDataService coreCaseDataService) {
        super(coreCaseDataService);
    }

    public List<CaseDetails> getApplications() {

        SearchResult searchResult = coreCaseDataService
            .searchGeneralApplication(query(START_INDEX));

        int pages = calculatePages(searchResult);
        List<CaseDetails> caseDetails = new ArrayList<>(searchResult.getCases());

        for (int i = 1; i < pages; i++) {
            SearchResult result = coreCaseDataService
                .searchGeneralApplication(query(i * ES_DEFAULT_SEARCH_LIMIT));
            caseDetails.addAll(result.getCases());
        }

        return caseDetails;
    }

    public Query query(int startIndex) {
        return new Query(
            boolQuery()
                .minimumShouldMatch(1)
                .should(boolQuery()
                            .must(rangeQuery("data.generalAppNotificationDeadlineDate").lt(LocalDate.now()
                                                                                              .atTime(LocalTime.MIN)
                                                                                              .toString()))
                            .mustNot(matchQuery("data.respondentResponseDeadlineChecked", "Yes"))
                            .must(beState(AWAITING_RESPONDENT_RESPONSE))),
            List.of("reference"),
            startIndex
        );
    }

    @Override
    Query query(final int startIndex, final CaseState caseState) {
        return null;
    }

    @Override
    Query queryForOrderMade(final int startIndex, final CaseState caseState, final GeneralApplicationTypes gaType) {
        return null;
    }

    @Override
    Query queryForBusinessProcessStatus(final int startIndex, final BusinessProcessStatus processStatus) {
        return null;
    }

    private QueryBuilder beState(CaseState caseState) {
        return boolQuery()
            .must(matchQuery("state", caseState.toString()));
    }
}
