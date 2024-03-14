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

    /*
     * Order dates are required to be pre-populated as follows
     *
     * calculate the any follow-up date/s from Next day
     * When the date calculation (result) fall on a non-working day (i.e. a bank holiday/weekend/privilege day)
     * - Then automatically move any calculated follow-up date/s to the next working/business day
     *
     * */
    public LocalDate getJudicialOrderDeadlineDate(LocalDateTime responseDate, int daysToAdd) {
        LocalDateTime dateTime = responseDate.plusDays(daysToAdd);

        LocalDate deadLineDate = calculateFirstWorkingDay(dateTime.toLocalDate());
        return deadLineDate;
    }
}
