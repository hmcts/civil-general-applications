package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;

@Service
@RequiredArgsConstructor
public class JudicialDecisionService {

    public static final String WRITTEN_REPRESENTATION_DATE_CANNOT_BE_IN_PAST =
        "The date entered cannot be in the past.";

    public List<String> validateWrittenRepresentationsDates(GAJudicialWrittenRepresentations
                                                                judicialWrittenRepresentationsDate) {
        List<String> errors = new ArrayList<>();
        if (judicialWrittenRepresentationsDate.getWrittenOption() == SEQUENTIAL_REPRESENTATIONS
            && judicialWrittenRepresentationsDate.getWrittenSequentailRepresentationsBy() != null) {
            LocalDate writtenSequentailRepresentationDate = judicialWrittenRepresentationsDate
                .getWrittenSequentailRepresentationsBy();
            if (LocalDate.now().isAfter(writtenSequentailRepresentationDate)) {
                errors.add(WRITTEN_REPRESENTATION_DATE_CANNOT_BE_IN_PAST);
            }
        }
        if (judicialWrittenRepresentationsDate.getWrittenOption() == CONCURRENT_REPRESENTATIONS
            && judicialWrittenRepresentationsDate.getWrittenConcurrentRepresentationsBy() != null) {
            LocalDate writtenConcurrentRepresentationDate = judicialWrittenRepresentationsDate
                .getWrittenConcurrentRepresentationsBy();
            if (LocalDate.now().isAfter(writtenConcurrentRepresentationDate)) {
                errors.add(WRITTEN_REPRESENTATION_DATE_CANNOT_BE_IN_PAST);
            }
        }
        return errors;
    }
}

