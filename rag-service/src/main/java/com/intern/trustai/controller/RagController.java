package com.intern.trustai.controller;


import com.intern.trustai.dto.ChunkResponse;
import com.intern.trustai.service.RagPipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagPipelineService ragService;

    public RagController(RagPipelineService ragService) {
        this.ragService = ragService;
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
            return ResponseEntity.internalServerError().body("Erreur lors de l'ingestion : " + e.getMessage());
        }
    }
    @GetMapping("/dashboard/metrics")
    @PreAuthorize("hasRole('admin')")
    public String getDashboardMetrics() {
        return "Statistiques globales renvoyées.";
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
