package com.intern.trustai.service;

public interface PdfReportService {

    byte[] generateAndSignReport(Long aiMessageId) throws Exception;
}
