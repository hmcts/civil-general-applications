package uk.gov.hmcts.reform.civil.repositories;

import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface HearingScheduledReferenceRepository {

    @SqlQuery("SELECT next_hearing_scheduled_reference_number()")
    String getHearingReferenceNumber();
}
