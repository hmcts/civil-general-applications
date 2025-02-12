package uk.gov.hmcts.reform.civil.service.search;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.math.RoundingMode.UP;

@RequiredArgsConstructor
public abstract class ElasticSearchService {

    protected final CoreCaseDataService coreCaseDataService;

    protected static final int START_INDEX = 0;
    protected static final int ES_DEFAULT_SEARCH_LIMIT = 10;

    public Set<CaseDetails> getGeneralApplications(CaseState caseState) {
        SearchResult searchResult = coreCaseDataService.searchGeneralApplication(query(START_INDEX, caseState));
        int pages = calculatePages(searchResult);
        List<CaseDetails> caseDetails = new ArrayList<>(searchResult.getCases());

        for (int i = 1; i < pages; i++) {
            SearchResult result = coreCaseDataService
                .searchGeneralApplication(query(i * ES_DEFAULT_SEARCH_LIMIT, caseState));
            caseDetails.addAll(result.getCases());
        }

        return new HashSet<>(caseDetails);
    }

    public Set<CaseDetails> getOrderMadeGeneralApplications(CaseState caseState, GeneralApplicationTypes gaType) {

        SearchResult searchResult = coreCaseDataService
            .searchGeneralApplication(queryForOrderMade(START_INDEX, caseState, gaType));

        int pages = calculatePages(searchResult);
        List<CaseDetails> caseDetails = new ArrayList<>(searchResult.getCases());

        for (int i = 1; i < pages; i++) {
            SearchResult result = coreCaseDataService
                .searchGeneralApplication(queryForOrderMade(i * ES_DEFAULT_SEARCH_LIMIT, caseState, gaType));
            caseDetails.addAll(result.getCases());
        }

        return new HashSet<>(caseDetails);
    }

    public Set<CaseDetails> getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus processStatus) {
        SearchResult searchResult = coreCaseDataService
            .searchGeneralApplication(queryForBusinessProcessStatus(START_INDEX, processStatus));
        int pages = calculatePages(searchResult);
        List<CaseDetails> caseDetails = new ArrayList<>(searchResult.getCases());

        for (int i = 1; i < pages; i++) {
            SearchResult result = coreCaseDataService
                .searchGeneralApplication(queryForBusinessProcessStatus(i * ES_DEFAULT_SEARCH_LIMIT, processStatus));
            caseDetails.addAll(result.getCases());
        }

        return new HashSet<>(caseDetails);
    }

    abstract Query query(int startIndex, CaseState caseState);

    abstract Query queryForOrderMade(int startIndex, CaseState caseState,
                                     GeneralApplicationTypes gaType);

    abstract Query queryForBusinessProcessStatus(int startIndex, BusinessProcessStatus processStatus);

    protected int calculatePages(SearchResult searchResult) {
        return new BigDecimal(searchResult.getTotal()).divide(new BigDecimal(ES_DEFAULT_SEARCH_LIMIT), UP).intValue();
    }
}
