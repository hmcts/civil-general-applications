package uk.gov.hmcts.reform.civil.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateFormatHelper {

    public static final String DATE_TIME_AT = "h:mma 'on' d MMMM yyyy";
    public static final String DATE = "d MMMM yyyy";
    public static final String MANDATORY_SUFFIX = ".000Z";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm.SSS'Z'",
                   Locale.ENGLISH);

    private DateFormatHelper() {
        //NO-OP
    }

    public static String formatLocalDateTime(LocalDateTime dateTime, String format) {
        return dateTime.format(DateTimeFormatter.ofPattern(format, Locale.UK));
    }

    public static String formatLocalDate(LocalDate date, String format) {
        return date.format(DateTimeFormatter.ofPattern(format, Locale.UK));
    }
}
