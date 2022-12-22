package uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class JudgeDecisionPdfDocument implements MappableObject {

    private final String claimNumber;
    private final String claimantName;
    private final String defendantName;
    private final String applicationType;
    private final String submittedOn;
    private final String judicialByCourtsInitiative;
    private final String locationName;

    private final String judicialByCourtsInitiativeListForHearing;
    private final String judicialByCourtsInitiativeForWrittenRep;
    private final String applicantName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonSerialize(using = LocalDateSerializer.class)
    private final LocalDate applicationDate;
    private final String judgeDirection;
    private final String dismissalOrder;
    private final String generalOrder;
    private final String requestOrder;
    private final String writtenOrder;
    private final String hearingLocation;
    private final String estimatedHearingLength;
    private final String reasonForDecision;
    private final String judgeRecital;
    private final String hearingOrder;
    private final LocalDate dateBy;
    private final String judgeComments;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonSerialize(using = LocalDateSerializer.class)
    private final LocalDate uploadDeadlineDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @JsonSerialize(using = LocalDateSerializer.class)
    private final LocalDate responseDeadlineDate;
}
