package uk.gov.hmcts.reform.civil.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.civil.callback.CallbackException;
import uk.gov.hmcts.reform.civil.request.RequestData;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ResourceExceptionHandlerTest {

    @Mock
    private RequestData requestData;

    @InjectMocks
    private ResourceExceptionHandler handler;

    @Test
    void shouldReturnNotFound_whenCallbackExceptionThrown() {
        testTemplate(
            "expected exception for missing callback handler",
            CallbackException::new,
            handler::notFound,
            HttpStatus.NOT_FOUND
        );
    }

    private <E extends Exception> void testTemplate(
        String message,
        Function<String, E> exceptionBuilder,
        Function<E, ResponseEntity<?>> method,
        HttpStatus expectedStatus
    ) {
        E exception = exceptionBuilder.apply(message);
        ResponseEntity<?> result = method.apply(exception);
        assertThat(result.getStatusCode()).isSameAs(expectedStatus);
        assertThat(result.getBody()).isNotNull()
            .extracting(Object::toString).asString().contains(message);
    }
}
