package com.intern.trustai.controller;


import com.intern.trustai.entity.Document;
import com.intern.trustai.repository.DocumentRepository;
import com.intern.trustai.repository.InteractionLogRepository;
import com.intern.trustai.dto.ChunkResponse;
import com.intern.trustai.dto.DashboardStatsResponse;
import com.intern.trustai.service.RagPipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "http://localhost:4200")
public class RagController {

    private final RagPipelineService ragService;
    private final DocumentRepository documentRepository;
    private final InteractionLogRepository interactionLogRepository;

    public RagController(RagPipelineService ragService, DocumentRepository documentRepository, InteractionLogRepository interactionLogRepository) {
        this.ragService = ragService;
        this.documentRepository = documentRepository;
        this.interactionLogRepository = interactionLogRepository;
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentRepository.findAll());
    }

    @GetMapping("/chat")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public String askQuestion() {
        return "Requête sémantique autorisée.";
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole( 'analyst', 'admin')")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            ragService.ingestFile(file);
            return ResponseEntity.ok("Document ingéré et vectorisé avec succès.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur lors de l'ingestion : " + e.getMessage());
        }
    }
    @GetMapping("/dashboard/metrics")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<DashboardStatsResponse> getDashboardMetrics() {
        long totalDocs = documentRepository.count();
        long totalReqs = interactionLogRepository.countTotalRequests();
        long totalToks = interactionLogRepository.sumTotalTokens();

        List<DashboardStatsResponse.HistoryEntry> history = interactionLogRepository.getRequestsHistory().stream()
                .map(obj -> DashboardStatsResponse.HistoryEntry.builder()
                        .date((String) obj[0])
                        .count(((Number) obj[1]).longValue())
                        .build())
                .toList();

        List<DashboardStatsResponse.TokenDistributionEntry> tokens = interactionLogRepository.getTokenDistribution().stream()
                .map(obj -> DashboardStatsResponse.TokenDistributionEntry.builder()
                        .model((String) obj[0])
                        .tokens(((Number) obj[1]).longValue())
                        .build())
                .toList();

        return ResponseEntity.ok(DashboardStatsResponse.builder()
                .totalDocuments(totalDocs)
                .totalRequests(totalReqs)
                .totalTokens(totalToks)
                .requestsHistory(history)
                .tokenDistribution(tokens)
                .build());
    }



    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<List<ChunkResponse>> searchContext(@RequestBody Map<String, Object> payload) {
        String query = (String) payload.get("query");
        // Récupérer le paramètre topK s'il existe, sinon 3 par défaut
        int topK = payload.containsKey("topK") ? (int) payload.get("topK") : 3;

        List<ChunkResponse> relevantChunks = ragService.searchSimilarChunks(query, topK);
        return ResponseEntity.ok(relevantChunks);
    }

}
