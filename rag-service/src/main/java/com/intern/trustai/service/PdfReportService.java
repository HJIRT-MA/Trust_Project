package com.intern.trustai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intern.trustai.entity.ChatMessage;
import com.intern.trustai.entity.HallucinationCheck;
import com.intern.trustai.entity.ReportSignature;
import com.intern.trustai.repository.ChatMessageRepository;
import com.intern.trustai.repository.HallucinationCheckRepository;
import com.intern.trustai.repository.ReportSignatureRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PdfReportService {

    private final ChatMessageRepository chatMessageRepository;
    private final HallucinationCheckRepository checkRepository;
    private final ReportSignatureRepository signatureRepository;
    private final PrivateKey privateKey;
    private final ObjectMapper objectMapper;

    public PdfReportService(ChatMessageRepository chatMessageRepository,
                            HallucinationCheckRepository checkRepository,
                            ReportSignatureRepository signatureRepository,
                            PrivateKey privateKey) {
        this.chatMessageRepository = chatMessageRepository;
        this.checkRepository = checkRepository;
        this.signatureRepository = signatureRepository;
        this.privateKey = privateKey;
        this.objectMapper = new ObjectMapper();
    }

    public byte[] generateAndSignReport(Long aiMessageId) throws Exception {
        ChatMessage aiMessage = chatMessageRepository.findById(aiMessageId)
                .orElseThrow(() -> new IllegalArgumentException("AI Message not found"));

        if (!"AI".equals(aiMessage.getRole())) {
            throw new IllegalArgumentException("Message ID does not belong to an AI response");
        }

        // Find the user question
        List<ChatMessage> conversationMessages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(aiMessage.getConversation().getId());
        
        String question = "Question not found";
        for (int i = 0; i < conversationMessages.size(); i++) {
            if (conversationMessages.get(i).getId().equals(aiMessageId) && i > 0) {
                ChatMessage previous = conversationMessages.get(i - 1);
                if ("USER".equals(previous.getRole())) {
                    question = previous.getContent();
                }
                break;
            }
        }

        List<HallucinationCheck> checks = checkRepository.findByMessageId(aiMessageId);

        // Generate PDF
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        document.add(new Paragraph("TrustAI Hallucination Guard Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18).setBold());

        // Global Score
        document.add(new Paragraph(new Text("Global Confidence Score: ").setBold())
                .add(new Text(aiMessage.getConfidenceScore() != null ? aiMessage.getConfidenceScore() + "%" : "N/A")));

        // Question
        document.add(new Paragraph(new Text("Question:\n").setBold()).add(question));

        // AI Response
        document.add(new Paragraph(new Text("AI Response:\n").setBold()).add(aiMessage.getContent()));

        // Claims and Sources
        document.add(new Paragraph(new Text("Factual Claims & Sources:\n").setBold()).setFontSize(14));
        
        if (checks.isEmpty()) {
            document.add(new Paragraph("No claims analyzed."));
        } else {
            for (HallucinationCheck check : checks) {
                document.add(new Paragraph(new Text("Claim: ").setBold()).add(check.getClaimText()));
                document.add(new Paragraph(new Text("Status: ").setBold()).add(check.getStatus())
                        .add(new Text(" (Score: " + String.format("%.2f", check.getSimilarityScore()) + ")")));
                
                // Parse source chunks
                try {
                    List<Map<String, Object>> chunks = objectMapper.readValue(check.getSourceChunks(),
                            new TypeReference<List<Map<String, Object>>>() {});
                    if (!chunks.isEmpty()) {
                        document.add(new Paragraph(new Text("Sources:\n").setItalic()));
                        for (int i = 0; i < chunks.size(); i++) {
                            Map<String, Object> c = chunks.get(i);
                            document.add(new Paragraph("  [" + (i + 1) + "] Score " + c.get("score") + ": " + c.get("text"))
                                    .setFontSize(10));
                        }
                    } else {
                        document.add(new Paragraph("  No sources found.").setFontSize(10));
                    }
                } catch (Exception e) {
                    document.add(new Paragraph("  Error parsing sources.").setFontSize(10));
                }
                document.add(new Paragraph("\n"));
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
        Optional<ReportSignature> existingOpt = signatureRepository.findByMessageId(aiMessageId);
        ReportSignature reportSignature = existingOpt.orElse(new ReportSignature());
        reportSignature.setMessage(aiMessage);
        reportSignature.setPdfHash(pdfHash);
        reportSignature.setSignature(signatureStr);
        signatureRepository.save(reportSignature);

        return pdfBytes;
    }
}
