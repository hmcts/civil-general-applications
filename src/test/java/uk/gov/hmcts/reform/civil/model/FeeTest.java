package uk.gov.hmcts.reform.civil.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class FeeTest {

    @Test
    void shouldTestEqualsAndHashCode() {
        Fee fee1 = new Fee(BigDecimal.valueOf(10000.00), "version1", "description1");
        Fee fee2 = new Fee(BigDecimal.valueOf(10000.00), "version1", "description1");
        Fee fee3 = new Fee(BigDecimal.valueOf(30000.00), "version2", "description2");

        assertAll(
            () -> assertEquals(fee1, fee2),
            () -> assertNotEquals(fee1, fee3),
            () -> assertEquals(fee1.hashCode(), fee2.hashCode()),
            () -> assertNotEquals(fee1.hashCode(), fee3.hashCode())
        );
    }
}
