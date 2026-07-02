package com.intern.trustai.controller;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @GetMapping("/chat")
    @PreAuthorize("hasAnyRole('viewer', 'analyst', 'admin')")
    public String askQuestion() {
        return "Requête sémantique autorisée.";
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole( 'analyst', 'admin')")
    public String uploadDocument() {
        return "Upload autorisé et indexation en cours.";
    }

    @GetMapping("/dashboard/metrics")
    @PreAuthorize("hasRole('admin')")
    public String getDashboardMetrics() {
        return "Statistiques globales renvoyées.";
    }

}
