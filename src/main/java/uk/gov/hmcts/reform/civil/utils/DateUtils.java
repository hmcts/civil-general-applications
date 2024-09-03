package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.enums.MonthNamesWelsh;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private DateUtils() {
        //No op
    }

    public static String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy");
        return date.format(formatter);
    }

    public static String formatDateInWelsh(LocalDate date) {
        String month = MonthNamesWelsh.getWelshNameByValue(date.getMonth().getValue());
        return date.getDayOfMonth() + " " + month + " " + date.getYear();
    }
}
