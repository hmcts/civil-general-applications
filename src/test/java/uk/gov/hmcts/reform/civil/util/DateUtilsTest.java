package uk.gov.hmcts.reform.civil.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.civil.utils.DateUtils;

import java.time.LocalDate;

class DateUtilsTest {

    @Test
    void testFormatDate() {
        LocalDate date = LocalDate.of(2024, 9, 04);
        String dateFormatted = DateUtils.formatDate(date);
        Assertions.assertEquals("4 September 2024", dateFormatted);
    }

    @Test
    void testFormatDateInWelsh() {
        LocalDate date = LocalDate.of(2024, 9, 04);
        String dateFormatted = DateUtils.formatDateInWelsh(date);
        Assertions.assertEquals("4 Medi 2024", dateFormatted);
    }
}
