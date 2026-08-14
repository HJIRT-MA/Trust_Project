package com.intern.trustai.controller;

import com.intern.trustai.service.PdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.intern.trustai.repository.ReportSignatureRepository;
import com.intern.trustai.dto.ReportHistoryDTO;
import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/guard")
@CrossOrigin(origins = "http://localhost:4200")
public class GuardController {

    private final PdfReportService pdfReportService;
    private final ReportSignatureRepository reportSignatureRepository;

    public GuardController(PdfReportService pdfReportService, ReportSignatureRepository reportSignatureRepository) {
        this.pdfReportService = pdfReportService;
        this.reportSignatureRepository = reportSignatureRepository;
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
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<List<ReportHistoryDTO>> getReportHistory() {
        List<ReportHistoryDTO> history = reportSignatureRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(sig -> new ReportHistoryDTO(
                        sig.getMessage().getId(),
                        sig.getCreatedAt().toString(),
                        sig.getMessage().getConfidenceScore(),
                        sig.getMessage().getConversation().getUserId()
                )).collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }
}
