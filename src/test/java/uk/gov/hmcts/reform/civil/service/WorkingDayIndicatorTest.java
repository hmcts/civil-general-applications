package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.service.bankholidays.PublicHolidaysCollection;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class WorkingDayIndicatorTest {

    private static final LocalDate BANK_HOLIDAY = LocalDate.of(2017, 5, 29);
    private static final LocalDate NEXT_WORKING_DAY_AFTER_BANK_HOLIDAY = LocalDate.of(2017, 5, 30);
    private static final LocalDate PREVIOUS_WORKING_DAY_BEFORE_BANK_HOLIDAY = LocalDate.of(2017, 5, 26);
    private static final LocalDate SATURDAY_WEEK_BEFORE = LocalDate.of(2017, 6, 3);
    private static final LocalDate SUNDAY_WEEK_BEFORE = LocalDate.of(2017, 6, 4);
    private static final LocalDate MONDAY = LocalDate.of(2017, 6, 5);
    private static final LocalDate TUESDAY = LocalDate.of(2017, 6, 6);
    private static final LocalDate WEDNESDAY = LocalDate.of(2017, 6, 7);
    private static final LocalDate THURSDAY = LocalDate.of(2017, 6, 8);
    private static final LocalDate FRIDAY = LocalDate.of(2017, 6, 9);
    private static final LocalDate SATURDAY = LocalDate.of(2017, 6, 10);
    private static final LocalDate SUNDAY = LocalDate.of(2017, 6, 11);

    static class WeekDaysArgumentProvider implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(MONDAY),
                Arguments.of(TUESDAY),
                Arguments.of(WEDNESDAY),
                Arguments.of(THURSDAY),
                Arguments.of(FRIDAY)
            );
        }
    }

    static class WeekendArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(SATURDAY),
                Arguments.of(SUNDAY)
            );
        }
    }

    private WorkingDayIndicator service;

    @Mock
    private PublicHolidaysCollection publicHolidaysApiClient;

    @BeforeEach
    void setup() {
        service = new WorkingDayIndicator(publicHolidaysApiClient);
    }

    @Nested
    class ForWeekdays {

        @ParameterizedTest
        @ArgumentsSource(WeekDaysArgumentProvider.class)
        void shouldReturnTrue_whenWeekday(LocalDate weekday) {
            when(publicHolidaysApiClient.getPublicHolidays()).thenReturn(Collections.emptySet());

            assertTrue(service.isWorkingDay(weekday));
        }
    }

    @Nested
    class ForWeekend {

        @ParameterizedTest
        @ArgumentsSource(WeekendArgumentsProvider.class)
        void shouldReturnFalse_whenWeekend(LocalDate weekend) {
            assertFalse(service.isWorkingDay(weekend));
        }
    }

    @Nested
    class IsWorkingDay {

        @Test
        void shouldReturnFalseForOneBankHoliday_whenThereIsOneBankHolidayInCollection() {
            when(publicHolidaysApiClient.getPublicHolidays())
                .thenReturn(new HashSet<>(singletonList(BANK_HOLIDAY)));

            assertFalse(service.isWorkingDay(BANK_HOLIDAY));
        }

        @Test
        void shouldReturnFalseForPublicHoliday_WhenThereIsMoreDatesInPublicHolidaysCollection() {
            Set<LocalDate> publicHolidays = new HashSet<>(Arrays.asList(MONDAY, TUESDAY, WEDNESDAY, THURSDAY));
            when(publicHolidaysApiClient.getPublicHolidays()).thenReturn(publicHolidays);

            assertAll(
                "Public holidays",
                () -> assertFalse(service.isWorkingDay(MONDAY)),
                () -> assertFalse(service.isWorkingDay(TUESDAY)),
                () -> assertFalse(service.isWorkingDay(WEDNESDAY)),
                () -> assertFalse(service.isWorkingDay(THURSDAY))
            );
        }
    }
}
