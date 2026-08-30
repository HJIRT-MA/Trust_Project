package com.intern.trustai.service;

import com.intern.trustai.entity.BlockchainProof;
import com.intern.trustai.repository.BlockchainProofRepository;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class AiActComplianceReportService {

    private final BlockchainProofRepository proofRepository;

    public AiActComplianceReportService(BlockchainProofRepository proofRepository) {
        this.proofRepository = proofRepository;
    }

    public byte[] generateComplianceReportPdf() throws Exception {
        List<BlockchainProof> proofs = proofRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        Paragraph title = new Paragraph("TrustAI - Rapport de Conformite AI Act")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setBold()
                .setMarginBottom(20);
        document.add(title);

        Paragraph subtitle = new Paragraph("Rapport genere le : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(subtitle);

        // Description
        document.add(new Paragraph("Ce document atteste de la tracabilite et de l'immuabilite des audits de securite realises par la plateforme TrustAI, conformement aux exigences de transparence du reglement europeen sur l'Intelligence Artificielle (AI Act)."));
        document.add(new Paragraph("Chaque ligne de ce rapport represente un evenement d'audit ancre publiquement sur la blockchain Polygon.").setMarginBottom(20));

        // Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{15, 20, 20, 30, 15}))
                .useAllAvailableWidth();

        table.addHeaderCell(new Cell().add(new Paragraph("ID Evenement").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Type").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Transaction (TxHash)").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Preuve Polygon").setBold()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (BlockchainProof proof : proofs) {
            table.addCell(new Cell().add(new Paragraph(proof.getEventId() != null ? proof.getEventId() : "N/A")));
            table.addCell(new Cell().add(new Paragraph(proof.getEventType() != null ? proof.getEventType() : "N/A")));
            table.addCell(new Cell().add(new Paragraph(sdf.format(new Date(proof.getTimestamp())))));
            
            String txHash = proof.getTxHash() != null ? proof.getTxHash() : "N/A";
            Cell txHashCell = new Cell().add(new Paragraph(txHash.length() > 20 ? txHash.substring(0, 20) + "..." : txHash).setFontSize(8));
            table.addCell(txHashCell);

            // Generate QR Code for PolygonScan if txHash is valid
            if (txHash != null && txHash.startsWith("0x")) {
                String polygonScanUrl = "https://polygonscan.com/tx/" + txHash;
                BarcodeQRCode qrCode = new BarcodeQRCode(polygonScanUrl);
                Image qrCodeImage = new Image(qrCode.createFormXObject(ColorConstants.BLACK, pdf))
                        .setWidth(40)
                        .setHeight(40)
                        .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                table.addCell(new Cell().add(qrCodeImage));
            } else {
                table.addCell(new Cell().add(new Paragraph("N/A")));
            }
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }
}
