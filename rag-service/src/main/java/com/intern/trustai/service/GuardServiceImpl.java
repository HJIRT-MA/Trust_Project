package com.intern.trustai.service;

import com.intern.trustai.dto.ReportHistoryDTO;
import com.intern.trustai.repository.ReportSignatureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GuardServiceImpl implements GuardService {

    private final ReportSignatureRepository reportSignatureRepository;

    public GuardServiceImpl(ReportSignatureRepository reportSignatureRepository) {
        this.reportSignatureRepository = reportSignatureRepository;
    }

    public List<ReportHistoryDTO> getReportHistory() {
        return reportSignatureRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(sig -> new ReportHistoryDTO(
                        sig.getMessage().getId(),
                        sig.getCreatedAt().toString(),
                        sig.getMessage().getConfidenceScore(),
                        sig.getMessage().getConversation().getUserId()
                )).collect(Collectors.toList());
    }
}
