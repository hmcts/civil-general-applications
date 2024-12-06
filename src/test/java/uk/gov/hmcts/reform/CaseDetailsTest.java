package uk.gov.hmcts.reform;

import uk.gov.hmcts.reform.ccd.client.model.Classification;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CaseDetailsTest {

    @Test
    void shouldBuildCaseDetailsWithAllFields() {
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastModified = LocalDateTime.now();
        Map<String, Object> caseData = Map.of("key", "value");

        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .caseTypeId("caseTypeId")
            .createdDate(createdDate)
            .lastModified(lastModified)
            .state("state")
            .lockedBy(123)
            .securityLevel(2)
            .data(caseData)
            .securityClassification(Classification.PUBLIC)
            .callbackResponseStatus("callbackStatus")
            .build();

        assertThat(caseDetails).isNotNull();
        assertThat(caseDetails.getId()).isEqualTo(1L);
        assertThat(caseDetails.getJurisdictionId()).isEqualTo("jurisdictionId");
        assertThat(caseDetails.getCaseTypeId()).isEqualTo("caseTypeId");
        assertThat(caseDetails.getCreatedDate()).isEqualTo(createdDate);
        assertThat(caseDetails.getLastModified()).isEqualTo(lastModified);
        assertThat(caseDetails.getState()).isEqualTo("state");
        assertThat(caseDetails.getLockedBy()).isEqualTo(123);
        assertThat(caseDetails.getSecurityLevel()).isEqualTo(2);
        assertThat(caseDetails.getData()).isEqualTo(caseData);
        assertThat(caseDetails.getSecurityClassification()).isEqualTo(Classification.PUBLIC);
        assertThat(caseDetails.getCallbackResponseStatus()).isEqualTo("callbackStatus");
    }

    @Test
    void shouldUpdateCaseDetailsUsingToBuilder() {
        CaseDetails original = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .build();

        CaseDetails updated = original.toBuilder()
            .caseTypeId("newCaseTypeId")
            .build();

        assertThat(updated.getCaseTypeId()).isEqualTo("newCaseTypeId");
        assertThat(updated.getJurisdictionId()).isEqualTo("jurisdictionId");
        assertThat(updated.getId()).isEqualTo(1L);
    }

    @Test
    void shouldVerifyEqualsAndHashCode() {
        CaseDetails caseDetails1 = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .build();

        CaseDetails caseDetails2 = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .build();

        assertThat(caseDetails1).isEqualTo(caseDetails2);
        assertThat(caseDetails1.hashCode()).isEqualTo(caseDetails2.hashCode());
    }

    @Test
    void shouldVerifyToString() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .build();

        assertThat(caseDetails.toString()).contains("CaseDetails");
        assertThat(caseDetails.toString()).contains("id=1");
        assertThat(caseDetails.toString()).contains("jurisdictionId=jurisdictionId");
    }

    @Test
    void shouldVerifyEqualsWithSameObject() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .build();

        assertThat(caseDetails).isEqualTo(caseDetails); // Same instance
    }

    @Test
    void shouldVerifyEqualsWithNull() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .build();

        assertThat(caseDetails).isNotEqualTo(null); // Null comparison
    }

    @Test
    void shouldVerifyEqualsWithDifferentType() {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId")
            .build();

        assertThat(caseDetails).isNotEqualTo("A String"); // Different class comparison
    }

    @Test
    void shouldVerifyEqualsWithDifferentFieldValues() {
        CaseDetails caseDetails1 = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId1")
            .build();

        CaseDetails caseDetails2 = CaseDetails.builder()
            .id(1L)
            .jurisdictionId("jurisdictionId2") // Different jurisdictionId
            .build();

        assertThat(caseDetails1).isNotEqualTo(caseDetails2);
    }

    @Test
    void shouldVerifyHashCodeWithDifferentFieldValues() {
        Map<String, Object> caseData1 = Map.of("key", "value1");
        Map<String, Object> caseData2 = Map.of("key", "value2");

        CaseDetails caseDetails1 = CaseDetails.builder()
            .id(1L)
            .data(caseData1)
            .build();

        CaseDetails caseDetails2 = CaseDetails.builder()
            .id(1L)
            .data(caseData2) // Different data map
            .build();

        assertThat(caseDetails1.hashCode()).isNotEqualTo(caseDetails2.hashCode());
    }
}
