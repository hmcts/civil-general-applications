package uk.gov.hmcts.reform.civil.helpers;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.utils.DateFormatterUtil.getFormattedDate;

public class DateFormatterUtilTest {

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_10TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-10T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("10th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_11TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-11T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("11th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_13TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-13T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("13th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_14TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-14T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("14th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_15TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-15T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("15th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_01TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-01T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("1st December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_02TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-02T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("2nd December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_03TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-03T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("3rd December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_04TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-04T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("4th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_05TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-05T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("5th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_30TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-30T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("30th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_22TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-22T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("22nd December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_20TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-20T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("20th December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_21TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-21T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("21st December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_23TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-23T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("23rd December 2023");
    }

    @Test
    void shouldReturnExpectedDateFormat_when_2023_12_27TFormatIsPassed() {
        Instant instant = Instant.parse("2023-12-27T18:35:24.00Z");

        assertThat(getFormattedDate(Date.from(instant)))
            .isEqualTo("27th December 2023");
    }

}
