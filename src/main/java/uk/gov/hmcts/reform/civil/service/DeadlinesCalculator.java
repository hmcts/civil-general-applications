package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DeadlinesCalculator {

    public static final LocalTime END_OF_BUSINESS_DAY = LocalTime.of(15, 59, 59);

    private final WorkingDayIndicator workingDayIndicator;

    public LocalDateTime calculateApplicantResponseDeadline(LocalDateTime responseDate, int days) {
        LocalDateTime dateTime = responseDate;
        if (is4pmOrAfter(responseDate)) {
            dateTime = responseDate.plusDays(1);
        }
        return calculateFirstWorkingDay(dateTime.toLocalDate()).plusDays(days).atTime(END_OF_BUSINESS_DAY);
    }

    public LocalDate calculateFirstWorkingDay(LocalDate date) {
        while (!workingDayIndicator.isWorkingDay(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean is4pmOrAfter(LocalDateTime dateOfService) {
        return dateOfService.getHour() >= 16;
    }
}
