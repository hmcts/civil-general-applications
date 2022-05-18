package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.service.bankholidays.PublicHolidaysCollection;

import java.time.LocalDate;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

@Service
@RequiredArgsConstructor
public class WorkingDayIndicator {

    private final PublicHolidaysCollection publicHolidaysCollection;

    /**
     * Verifies if given date is a working day in UK (England and Wales only).
     */
    public boolean isWorkingDay(LocalDate date) {
        return !isWeekend(date)
            && !isPublicHoliday(date);
    }

    public boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
    }

    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidaysCollection.getPublicHolidays().contains(date);
    }
}
