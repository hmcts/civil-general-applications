package uk.gov.hmcts.reform.civil.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateFormatterUtil {

    private DateFormatterUtil() {
        //NO-OP
    }

    public static String getFormattedDate(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DATE);

        if (day >= 11 && day <= 13) {
            return new SimpleDateFormat("d'th' MMMM yyyy").format(date);
        }

        switch (day % 10) {
            case 1:
                return new SimpleDateFormat("d'st' MMMM yyyy").format(date);
            case 2:
                return new SimpleDateFormat("d'nd' MMMM yyyy").format(date);
            case 3:
                return new SimpleDateFormat("d'rd' MMMM yyyy").format(date);
            default:
                return new SimpleDateFormat("d'th' MMMM yyyy").format(date);
        }
    }
}
