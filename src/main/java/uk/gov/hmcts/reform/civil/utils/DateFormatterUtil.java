package uk.gov.hmcts.reform.civil.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateFormatterUtil {

    private DateFormatterUtil() {
        //NO-OP
    }

    public static String getFormattedDate(Date date){
        Calendar cal=Calendar.getInstance();
        cal.setTime(date);
        int day=cal.get(Calendar.DATE);

        if(!((day>10) && (day<19)))
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
        return new SimpleDateFormat("d'th' MMMM yyyy").format(date);
    }
}
