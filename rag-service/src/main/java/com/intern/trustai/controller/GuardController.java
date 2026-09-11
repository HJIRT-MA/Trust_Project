package com.intern.trustai.controller;

import com.intern.trustai.service.PdfReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.intern.trustai.dto.ReportHistoryDTO;
import java.util.List;
@RestController
@RequestMapping("/api/guard")
public class GuardController {

    private static final Logger log = LoggerFactory.getLogger(GuardController.class);

    private final PdfReportService pdfReportService;
    private final com.intern.trustai.service.GuardService guardService;

    public GuardController(PdfReportService pdfReportService, com.intern.trustai.service.GuardService guardService) {
        this.pdfReportService = pdfReportService;
        this.guardService = guardService;
    }

    @GetMapping("/report/{id}/pdf")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<byte[]> getGuardReportPdf(@PathVariable("id") Long id) {
        try {
            byte[] pdfBytes = pdfReportService.generateAndSignReport(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "guard_report_" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to generate guard report PDF for message {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<List<ReportHistoryDTO>> getReportHistory() {
        return ResponseEntity.ok(guardService.getReportHistory());
    }
}
