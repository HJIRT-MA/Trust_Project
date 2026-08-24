package com.intern.trustai.service;

import com.intern.trustai.entity.AuditFinding;
import com.intern.trustai.entity.AuditReportSignature;
import com.intern.trustai.entity.SmartContract;
import com.intern.trustai.repository.AuditFindingRepository;
import com.intern.trustai.repository.AuditReportSignatureRepository;
import com.intern.trustai.repository.SmartContractRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class SecurityPdfReportService {

    private final SmartContractRepository smartContractRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final AuditReportSignatureRepository signatureRepository;
    private final PrivateKey privateKey;

    public SecurityPdfReportService(SmartContractRepository smartContractRepository,
                                    AuditFindingRepository auditFindingRepository,
                                    AuditReportSignatureRepository signatureRepository,
                                    PrivateKey privateKey) {
        this.smartContractRepository = smartContractRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.signatureRepository = signatureRepository;
        this.privateKey = privateKey;
    }

    public byte[] generateAndSignReport(Long contractId) throws Exception {
        SmartContract contract = smartContractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Smart Contract not found"));

        List<AuditFinding> findings = auditFindingRepository.findBySmartContractId(contractId);

        // Generate PDF
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        document.add(new Paragraph("TrustAI Smart Contract Security Audit Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20).setBold().setFontColor(ColorConstants.BLUE));
        
        document.add(new Paragraph("\n"));

        // Executive Summary
        document.add(new Paragraph("Executive Summary").setFontSize(16).setBold().setUnderline());
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
        
        summaryTable.addCell(new Cell().add(new Paragraph("Contract Name:").setBold()));
        summaryTable.addCell(new Cell().add(new Paragraph(contract.getName())));
        
        summaryTable.addCell(new Cell().add(new Paragraph("Date:").setBold()));
        summaryTable.addCell(new Cell().add(new Paragraph(contract.getCreatedAt() != null ? contract.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A")));
        
        summaryTable.addCell(new Cell().add(new Paragraph("Auditor:").setBold()));
        summaryTable.addCell(new Cell().add(new Paragraph(contract.getAuditor() != null ? contract.getAuditor() : "Automated System")));
        
        summaryTable.addCell(new Cell().add(new Paragraph("Global Risk Score:").setBold()));
        summaryTable.addCell(new Cell().add(new Paragraph(contract.getGlobalRiskScore() + "/100")));
        
        summaryTable.addCell(new Cell().add(new Paragraph("Risk Level:").setBold()));
        summaryTable.addCell(new Cell().add(new Paragraph(contract.getRiskLevel() != null ? contract.getRiskLevel() : "UNKNOWN")
                .setFontColor(getRiskColor(contract.getRiskLevel()))));
                
        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        // Findings Details
        document.add(new Paragraph("Audit Findings Detailed Report").setFontSize(16).setBold().setUnderline());
        document.add(new Paragraph("\n"));

        if (findings.isEmpty()) {
            document.add(new Paragraph("No vulnerabilities detected in this contract."));
        } else {
            for (int i = 0; i < findings.size(); i++) {
                AuditFinding f = findings.get(i);
                
                document.add(new Paragraph("Finding #" + (i + 1) + ": " + f.getTitle())
                        .setFontSize(14).setBold().setFontColor(ColorConstants.DARK_GRAY));
                
                document.add(new Paragraph(new Text("Severity: ").setBold())
                        .add(new Text(f.getSeverity()).setFontColor(getRiskColor(f.getSeverity()))));
                
                if (f.getSwcId() != null) {
                    document.add(new Paragraph(new Text("SWC Reference: ").setBold())
                            .add(f.getSwcId() + " - " + f.getSwcTitle()));
                }

                document.add(new Paragraph(new Text("Description: ").setBold()).add(f.getDescription()));
                
                if (f.getEnrichedExplanation() != null && !f.getEnrichedExplanation().isEmpty()) {
                    document.add(new Paragraph(new Text("AI Enrichment / Remediation: ").setBold())
                            .add(f.getEnrichedExplanation()).setItalic());
                }

                if (f.getCodeSnippet() != null && !f.getCodeSnippet().isEmpty()) {
                    document.add(new Paragraph("Vulnerable Code:").setBold());
                    document.add(new Paragraph(f.getCodeSnippet())
                            .setFontSize(10).setFontColor(ColorConstants.RED)
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                }
                
                document.add(new Paragraph("--------------------------------------------------\n"));
            }
        }

        document.close();
        byte[] pdfBytes = baos.toByteArray();

        // SHA-256 Hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(pdfBytes);
        String pdfHash = Base64.getEncoder().encodeToString(hashBytes);

        // Digital Signature
        Signature rsaSign = Signature.getInstance("SHA256withRSA");
        rsaSign.initSign(privateKey);
        rsaSign.update(hashBytes);
        byte[] signatureBytes = rsaSign.sign();
        String signatureStr = Base64.getEncoder().encodeToString(signatureBytes);

        // Save to DB
        Optional<AuditReportSignature> existingOpt = signatureRepository.findBySmartContractId(contractId);
        AuditReportSignature reportSignature = existingOpt.orElse(new AuditReportSignature());
        reportSignature.setSmartContract(contract);
        reportSignature.setPdfHash(pdfHash);
        reportSignature.setSignature(signatureStr);
        signatureRepository.save(reportSignature);

        return pdfBytes;
    }
    
    private com.itextpdf.kernel.colors.Color getRiskColor(String level) {
        if (level == null) return ColorConstants.BLACK;
        switch (level.toUpperCase()) {
            case "CRITICAL": return ColorConstants.RED;
            case "HIGH": return ColorConstants.ORANGE;
            case "MEDIUM": return ColorConstants.YELLOW;
            case "LOW": return ColorConstants.BLUE;
            case "SAFE": return ColorConstants.GREEN;
            case "RISKY": return ColorConstants.ORANGE;
            default: return ColorConstants.BLACK;
        }
    }
}
