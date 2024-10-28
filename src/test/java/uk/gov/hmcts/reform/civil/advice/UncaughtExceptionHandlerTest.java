package uk.gov.hmcts.reform.civil.advice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.civil.request.RequestData;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UncaughtExceptionHandlerTest {

    public static final String NULL_POINTER_EXCEPTION = "Null pointer Exception!!";

    @Mock
    private RequestData requestData;

    @InjectMocks
    private UncaughtExceptionHandler uncaughtExceptionHandler;

    @Test
    void shouldReturnInternalServerError_whenUncaughtExceptionThrown() {
        Exception e = new NullPointerException(NULL_POINTER_EXCEPTION);
        ResponseEntity<?> result = uncaughtExceptionHandler.runtimeException(e);

        assertThat(result.getStatusCode()).isSameAs(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isNotNull()
            .extracting(Object::toString).asString().contains(NULL_POINTER_EXCEPTION);
    }

}