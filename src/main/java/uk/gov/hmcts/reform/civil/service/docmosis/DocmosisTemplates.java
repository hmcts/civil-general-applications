package uk.gov.hmcts.reform.civil.service.docmosis;

public enum DocmosisTemplates {
    GENERAL_ORDER("CV-UNS-GAP-ENG-01068.docx", "General_order_for_application_%s.pdf"),
    DIRECTION_ORDER("CV-UNS-GAP-ENG-01073.docx", "Direction_order_for_application_%s.pdf"),
    DISMISSAL_ORDER("CV-UNS-GAP-ENG-01067.docx", "Dismissal_order_for_application_%s.pdf"),
    REQUEST_FOR_INFORMATION("CV-UNS-GAP-ENG-01072.docx", "Request_for_information_for_application_%s.pdf"),
    HEARING_ORDER("CV-UNS-GAP-ENG-01069.docx", "Hearing_order_for_application_%s.pdf"),
    WRITTEN_REPRESENTATION_SEQUENTIAL("CV-UNS-GAP-ENG-01070.docx",
                                      "Order_Written_Representation_Sequential_for_application_%s.pdf"),
    WRITTEN_REPRESENTATION_CONCURRENT("CV-UNS-GAP-ENG-01071.docx",
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
