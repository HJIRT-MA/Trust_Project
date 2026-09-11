package com.intern.trustai.service;

public interface SecurityPdfReportService {

    byte[] generateAndSignReport(Long contractId) throws Exception;
}
