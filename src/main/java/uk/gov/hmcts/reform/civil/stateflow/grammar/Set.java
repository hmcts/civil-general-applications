package uk.gov.hmcts.reform.civil.stateflow.grammar;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import uk.gov.hmcts.reform.civil.model.CaseData;

/**
 * Represents the SET clause.
 */
public interface Set<S> {

    SetNext<S> set(Consumer<Map<String, Boolean>> flags);

    SetNext<S> set(BiConsumer<CaseData, Map<String, Boolean>> flags);
}
