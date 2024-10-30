package uk.gov.hmcts.reform.civil.config.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncHandlerPropertiesTest {

    @Test
    void shouldSetAndGetCorePoolSize() {
        AsyncHandlerProperties properties = new AsyncHandlerProperties();
        int expectedCorePoolSize = 5;

        properties.setCorePoolSize(expectedCorePoolSize);

        assertThat(properties.getCorePoolSize()).isEqualTo(expectedCorePoolSize);
    }

    @Test
    void shouldSetAndGetMaxPoolSize() {
        AsyncHandlerProperties properties = new AsyncHandlerProperties();
        int expectedMaxPoolSize = 10;

        properties.setMaxPoolSize(expectedMaxPoolSize);

        assertThat(properties.getMaxPoolSize()).isEqualTo(expectedMaxPoolSize);
    }

    @Test
    void shouldSetAndGetQueueCapacity() {
        AsyncHandlerProperties properties = new AsyncHandlerProperties();
        int expectedQueueCapacity = 50;

        properties.setQueueCapacity(expectedQueueCapacity);

        assertThat(properties.getQueueCapacity()).isEqualTo(expectedQueueCapacity);
    }

    @Test
    void shouldHaveProperToStringImplementation() {
        AsyncHandlerProperties properties = new AsyncHandlerProperties();
        properties.setCorePoolSize(2);
        properties.setMaxPoolSize(4);
        properties.setQueueCapacity(100);

        String toStringResult = properties.toString();

        assertThat(toStringResult).contains("corePoolSize=2");
        assertThat(toStringResult).contains("maxPoolSize=4");
        assertThat(toStringResult).contains("queueCapacity=100");
    }

    @Test
    void shouldVerifyEqualsAndHashCode() {
        AsyncHandlerProperties properties1 = new AsyncHandlerProperties();
        properties1.setCorePoolSize(2);
        properties1.setMaxPoolSize(4);
        properties1.setQueueCapacity(100);

        AsyncHandlerProperties properties2 = new AsyncHandlerProperties();
        properties2.setCorePoolSize(2);
        properties2.setMaxPoolSize(4);
        properties2.setQueueCapacity(100);

        AsyncHandlerProperties properties3 = new AsyncHandlerProperties();
        properties3.setCorePoolSize(3);

        assertThat(properties1).isEqualTo(properties2);
        assertThat(properties1.hashCode()).isEqualTo(properties2.hashCode());

        assertThat(properties1).isNotEqualTo(properties3);
        assertThat(properties1.hashCode()).isNotEqualTo(properties3.hashCode());
    }
}
