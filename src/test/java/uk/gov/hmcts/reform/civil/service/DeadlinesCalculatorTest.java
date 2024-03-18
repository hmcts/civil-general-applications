package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.helpers.ResourceReader;
import uk.gov.hmcts.reform.civil.service.bankholidays.BankHolidays;
import uk.gov.hmcts.reform.civil.service.bankholidays.BankHolidaysApi;
import uk.gov.hmcts.reform.civil.service.bankholidays.PublicHolidaysCollection;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.DeadlinesCalculator.END_OF_BUSINESS_DAY;

@ExtendWith(SpringExtension.class)
public class DeadlinesCalculatorTest {

    @Mock
    private BankHolidaysApi bankHolidaysApi;

    private DeadlinesCalculator calculator;

    private static final int PLUS_7DAYS = 7;

    @BeforeEach
    public void setUp() throws IOException {
        WorkingDayIndicator workingDayIndicator = new WorkingDayIndicator(
            new PublicHolidaysCollection(bankHolidaysApi)
        );
        when(bankHolidaysApi.retrieveAll()).thenReturn(loadFixture());

        calculator = new DeadlinesCalculator(workingDayIndicator);
    }

    /**
     * The fixture is taken from the real bank holidays API.
     */
    private BankHolidays loadFixture() throws IOException {
        String input = ResourceReader.readString("/bank-holidays.json");
        return new ObjectMapper().readValue(input, BankHolidays.class);
    }

    @Nested
    class ApplicantResponseDeadlineDates {

        @Test
        void shouldReturnDeadlinePlus2Days_whenResponseDateIsWeekday() {
            LocalDateTime weekdayDate = LocalDate.of(2022, 2, 15).atTime(12, 0);
            LocalDateTime expectedDeadline = weekdayDate.toLocalDate().plusDays(2).atTime(END_OF_BUSINESS_DAY);
            LocalDateTime responseDeadline = calculator.calculateApplicantResponseDeadline(weekdayDate, 2);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }

        @Test
        void shouldReturnDeadlinePlus3Days_whenResponseDateIsWeekday() {
            LocalDateTime weekdayDate = LocalDate.of(2022, 2, 15).atTime(12, 0);
            LocalDateTime expectedDeadline = weekdayDate.toLocalDate().plusDays(3).atTime(END_OF_BUSINESS_DAY);
            LocalDateTime responseDeadline = calculator.calculateApplicantResponseDeadline(weekdayDate, 3);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }

        @Test
        void shouldReturnDeadlinePlus8Days_whenResponseDateIsBankHoliday() {
            LocalDateTime weekdayDate = LocalDate.of(2020, 8, 31).atTime(8, 0);
            LocalDateTime expectedDeadline = weekdayDate.toLocalDate().plusDays(8).atTime(END_OF_BUSINESS_DAY);
            LocalDateTime responseDeadline = calculator.calculateApplicantResponseDeadline(weekdayDate, 5);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }

        @Test
        void shouldReturnDeadlinePlus8Days_whenResponseDateIsWeekEnd() {
            LocalDateTime weekdayDate = LocalDate.of(2022, 10, 1).atTime(8, 0);
            LocalDateTime expectedDeadline = weekdayDate.toLocalDate().plusDays(8).atTime(END_OF_BUSINESS_DAY);
            LocalDateTime responseDeadline = calculator.calculateApplicantResponseDeadline(weekdayDate, 5);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }

        @Test
        void judgeOrderReturnDeadlinePlus1Day_whenResponseDateIsMonday() {
            LocalDateTime weekdayDate = LocalDate.of(2024, 02, 05).atTime(2, 0);
            LocalDate expectedDeadline = weekdayDate.toLocalDate().plusDays(7);
            LocalDate responseDeadline = calculator.getJudicialOrderDeadlineDate(weekdayDate, PLUS_7DAYS);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }

        @Test
        void judgeOrderReturnDeadlinePlus1Day_whenResponseDateIsWeekday() {
            LocalDateTime weekdayDate = LocalDate.of(2024, 03, 06).atTime(2, 0);
            LocalDate expectedDeadline = weekdayDate.toLocalDate().plusDays(7);
            LocalDate responseDeadline = calculator.getJudicialOrderDeadlineDate(weekdayDate, PLUS_7DAYS);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }

        @Test
        void judgeOrderReturnDeadlinePlus1day_whenResponseDateIsSaturday() {
            LocalDateTime weekdayDate = LocalDate.of(2024, 02, 10).atTime(2, 0);
            LocalDate expectedDeadline = weekdayDate.toLocalDate().plusDays(9);
            LocalDate responseDeadline = calculator.getJudicialOrderDeadlineDate(weekdayDate, PLUS_7DAYS);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }

        @Test
        void judgeOrderReturnDeadlinePlus1day_whenResponseDateIsSunday() {
            LocalDateTime weekdayDate = LocalDate.of(2024, 03, 03).atTime(2, 0);
            LocalDate expectedDeadline = weekdayDate.toLocalDate().plusDays(8);
            LocalDate responseDeadline = calculator.getJudicialOrderDeadlineDate(weekdayDate, PLUS_7DAYS);

            assertThat(responseDeadline).isEqualTo(expectedDeadline);
        }
    }

}
