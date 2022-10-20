package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DeadlinesCalculator {

    public static final LocalTime END_OF_BUSINESS_DAY = LocalTime.of(16, 0, 0);
    public static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    private final WorkingDayIndicator workingDayIndicator;

    public LocalDate calculateFirstWorkingDay(LocalDate date) {
        while (!workingDayIndicator.isWorkingDay(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    public LocalDateTime calculateApplicantResponseDeadline(LocalDateTime responseDate, int daysToAdd) {
        LocalDateTime dateTime = responseDate;
        if (checkIf4pmOrAfter(responseDate)) {
            dateTime = responseDate.plusDays(1);
        }

        LocalDate startDate = calculateFirstWorkingDay(dateTime.toLocalDate());
        LocalDate endDate = startDate.plusDays(daysToAdd);

        long noOfHoliday = startDate.datesUntil(endDate.plusDays(1)).filter(data -> !workingDayIndicator
                .isWorkingDay(data)).count();

        return endDate.plusDays(noOfHoliday).atTime(END_OF_BUSINESS_DAY);
    }

    private boolean checkIf4pmOrAfter(LocalDateTime dateOfService) {
        return dateOfService.getHour() >= 16;
    }
}
