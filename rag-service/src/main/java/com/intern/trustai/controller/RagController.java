package com.intern.trustai.controller;


import com.intern.trustai.entity.Document;
import com.intern.trustai.repository.DocumentRepository;
import com.intern.trustai.dto.ChunkResponse;
import com.intern.trustai.service.RagPipelineService;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.intern.trustai.entity.Conversation;
import com.intern.trustai.entity.ChatMessage;
import com.intern.trustai.repository.ConversationRepository;
import com.intern.trustai.repository.ChatMessageRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.intern.trustai.dto.DashboardStatsDTO;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "http://localhost:4200")
public class RagController {

    private final RagPipelineService ragService;
    private final ResourcePatternResolver resourcePatternResolver;

    public RagController(RagPipelineService ragService, ResourcePatternResolver resourcePatternResolver) {
        this.ragService = ragService;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(ragService.getAllDocuments());
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('viewer','admin', 'analyst')")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> payload) {
        String query= (String) payload.get("query");
        int topK = payload.containsKey(("topK")) ? (int) payload.get("topK") : 3;

        Long conversationId = null;
        if(payload.containsKey("conversationId") && payload.get("conversationId") != null) {

            conversationId = Long.valueOf(payload.get("conversationId").toString());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId= authentication.getName();

        Map<String,Object> response = ragService.chatWithDocuments(query,topK,conversationId,userId);

        return ResponseEntity.ok(response);

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
    public DashboardStatsDTO getDashboardMetrics() {
        return ragService.getDashboardStats();
    }



   @DeleteMapping("/documents/{id}")
   @PreAuthorize("hasRole('admin')")
   public ResponseEntity<String> deleteDocument(@PathVariable("id") Long id) {
        try {
            ragService.deleteDocument(id);
            return ResponseEntity.ok("Document supprimé avec succès.");
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur lors de le suppression : " + e.getMessage());
        }
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

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<List<Conversation>> getConversations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        return ResponseEntity.ok(ragService.getUserConversations(userId));
    }


    @GetMapping("/conversations/{id}")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<List<ChatMessage>> getConversationMessages(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        try {
            return ResponseEntity.ok(ragService.getConversationMessages(id, userId));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Unauthorized to access this conversation")) {
                return ResponseEntity.status(403).build();
            }
            throw e;
        }
    }

    @DeleteMapping("/conversations/{id}")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<String> deleteConversation(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        try {
            ragService.deleteConversation(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Unauthorized to access this conversation")) {
                return ResponseEntity.status(403).build();
            }
            throw e;
        }
    }

    @GetMapping("/conversations/{id}/pdf")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public ResponseEntity<byte[]> getConversationPDF(@PathVariable("id") Long id) {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentification.getName();
        try{
            byte[] pdfBytes = ragService.generatePdfForConversation(id, userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "conversation_"+id+".pdf");
            headers.setCacheControl("must-revalidate,post-check=0, pre-check=0");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

}
