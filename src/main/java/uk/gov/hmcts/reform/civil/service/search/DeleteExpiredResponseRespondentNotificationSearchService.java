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
                                                                                              .toString()))),
            List.of("reference"),
            startIndex
        );
    }

    private QueryBuilder beState(CaseState caseState) {
        return boolQuery()
            .must(matchQuery("state", caseState.toString()));
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
}

