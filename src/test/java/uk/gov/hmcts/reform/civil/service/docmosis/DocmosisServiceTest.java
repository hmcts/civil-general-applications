package uk.gov.hmcts.reform.civil.service.docmosis;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DocmosisService.class
})
public class DocmosisServiceTest {

    private static final String POST_CODE = "ABC EDF";
    @Autowired
    private DocmosisService docmosisService;
    @MockBean
    private IdamClient idamClient;
    @MockBean
    private GeneralAppLocationRefDataService generalAppLocationRefDataService;

    private static final List<LocationRefData> locationRefData = Arrays
        .asList(LocationRefData.builder().epimmsId("1").venueName("Reading").postcode("E456 DiX0").build(),
                LocationRefData.builder().epimmsId("2").venueName("London").postcode(POST_CODE).build(),
                LocationRefData.builder().epimmsId("3").venueName("Manchester").postcode("d15 4567L").build());

    @Test
    void shouldReturnVenueName() {
        when(generalAppLocationRefDataService.getCourtLocations(any())).thenReturn(locationRefData);
        List<DynamicListElement> listItems = Arrays.asList(DynamicListElement.builder().code("code").label("label").build());

        DynamicListElement selectedLocation = DynamicListElement
            .builder().label("Court - London - EPIMMDS CODE - " + POST_CODE).build();

        CaseData caseData = CaseData.builder()
            .judicialListForHearing(
                GAJudgesHearingListGAspec.builder()
                    .hearingPreferencesPreferredType(GAJudicialHearingType.IN_PERSON)
                    .hearingPreferredLocation(
                        DynamicList.builder().value(selectedLocation).listItems(listItems).build()).build()).build();

        String venueName = docmosisService.populateJudicialHearingLocationVenueName(caseData, "auth");
        assertThat(venueName)
            .isEqualTo("London");
    }

    @Test
    void shouldThrowExceptionWhenNoLocationMatchForVenuePostCode() {
        when(generalAppLocationRefDataService.getCourtLocations(any())).thenReturn(locationRefData);
        List<DynamicListElement> listItems = Arrays.asList(DynamicListElement.builder().code("code").label("label").build());

        DynamicListElement selectedLocation = DynamicListElement
            .builder().label("Court - London - EPIMMDS CODE - D08 EXIP").build();

        CaseData caseData = CaseData.builder()
            .judicialListForHearing(
                GAJudgesHearingListGAspec.builder()
                    .hearingPreferencesPreferredType(GAJudicialHearingType.IN_PERSON)
                    .hearingPreferredLocation(
                        DynamicList.builder().value(selectedLocation).listItems(listItems).build()).build()).build();

        Exception exception =
            assertThrows(IllegalArgumentException.class, ()
                -> docmosisService.populateJudicialHearingLocationVenueName(caseData, "auth"));
        String expectedMessage = "Venue Name is not found in location data";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void shouldReturnLocationRefData() {
        when(generalAppLocationRefDataService.getCourtLocations(any())).thenReturn(locationRefData);

        CaseData caseData = CaseData.builder()
            .caseManagementLocation(GACaseLocation.builder().baseLocation("2").build()).build();
        LocationRefData locationRefData = docmosisService.getCaseManagementLocationVenueName(caseData, "auth");
        assertThat(locationRefData.getVenueName())
            .isEqualTo("London");
    }

    @Test
    void shouldThrowExceptionWhenNoLocationMatch() {
        when(generalAppLocationRefDataService.getCourtLocations(any())).thenReturn(locationRefData);

        CaseData caseData = CaseData.builder()
            .caseManagementLocation(GACaseLocation.builder().baseLocation("8").build()).build();

        Exception exception =
            assertThrows(IllegalArgumentException.class, ()
                -> docmosisService.getCaseManagementLocationVenueName(caseData, "auth"));
        String expectedMessage = "Court Name is not found in location data";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void shouldRetunJudgeFullName() {
        when(idamClient
                 .getUserDetails(any()))
            .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());

        assertThat(docmosisService.getJudgeNameTitle("auth")).isEqualTo("John Doe");

    }

    @Test
    void shouldPopulateJudgeReason() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .reasonForDecisionText("Test Reason")
                                                      .showReasonForDecision(YesOrNo.YES).build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudgeReason(updateData)).isEqualTo("Test Reason");
    }

    @Test
    void shouldReturnEmptyJudgeReason() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .reasonForDecisionText("Test Reason")
                                                      .showReasonForDecision(YesOrNo.NO).build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudgeReason(updateData)).isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldReturn_EmptyString_JudgeCourtsInitiative_Option3() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .judicialByCourtsInitiative(
                                                          GAByCourtsInitiativeGAspec.OPTION_3).build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudicialByCourtsInitiative(updateData)).isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldPopulate_JudgeCourtsInitiative_Option2() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .orderWithoutNotice("abcdef")
                                                      .orderWithoutNoticeDate(LocalDate.now())
                                                      .judicialByCourtsInitiative(
                                                          GAByCourtsInitiativeGAspec.OPTION_2)
                                                      .build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudicialByCourtsInitiative(updateData))
            .isEqualTo("abcdef ".concat(LocalDate.now().format(DATE_FORMATTER)));
    }

    @Test
    void shouldPopulate_JudgeCourtsInitiative_Option1() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .orderCourtOwnInitiative("abcdef")
                                                      .orderCourtOwnInitiativeDate(LocalDate.now())
                                                      .judicialByCourtsInitiative(
                                                          GAByCourtsInitiativeGAspec.OPTION_1)
                                                      .build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudicialByCourtsInitiative(updateData))
            .isEqualTo("abcdef ".concat(LocalDate.now().format(DATE_FORMATTER)));
    }

}
