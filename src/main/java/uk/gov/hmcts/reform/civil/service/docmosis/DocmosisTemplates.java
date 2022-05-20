package uk.gov.hmcts.reform.civil.service.docmosis;

public enum DocmosisTemplates {
    GENERAL_ORDER("GeneralOrder.docx", "General_order_for_application_%s.pdf"),
    DIRECTION_ORDER("Directionorder.docx", "Direction_order_for_application_%s.pdf"),
    DISMISSAL_ORDER("DismissalOrder.docx", "Dismissal_order_for_application_%s.pdf"),
    REQUEST_FOR_INFORMATION("Requestforinformation.docx", "Request_for_information_for_application_%s.pdf"),
    HEARING_ORDER("HearingOrder.docx", "Hearing_order_for_application_%s.pdf"),
    WRITTEN_REPRESENTATION_SEQUENTIAL("OrderWrittenReps.docx",
                                      "Order_Written_Representation_Sequential_for_application_%s.pdf"),
    WRITTEN_REPRESENTATION_CONCURRENT("OrderWrittenRepsConcurrent.docx",
                                      "Order_Written_Representation_Concurrent_for_application_%s.pdf");

    private final String template;
    private final String documentTitle;

    DocmosisTemplates(String template, String documentTitle) {
        this.template = template;
        this.documentTitle = documentTitle;
    }

    public String getTemplate() {
        return template;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }
}
