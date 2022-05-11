package uk.gov.hmcts.reform.civil.service.docmosis;

public enum DocmosisTemplates {
    GENERAL_ORDER("GeneralOrder.docx", "General_order.pdf"),
    DIRECTION_ORDER("Directionorder.docx", "Direction_order.pdf"),
    DISMISSAL_ORDER("DismissalOrder.docx", "Dismissal_order.pdf"),
    REQUEST_FOR_INFORMATION("Requestforinformation.docx", "Request_for_information.pdf"),
    HEARING_ORDER("HearingOrder.docx", "Hearing_order.pdf"),
    WRITTEN_REP_SEQUENTIAL("OrderWrittenReps.docx", "Order_Written_Representation_Sequential.pdf"),
    WRITTEN_REP_CONCURRENT("OrderWrittenRepsConcurrent.docx", "Order_Written_Representation_Concurrent.pdf");

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
