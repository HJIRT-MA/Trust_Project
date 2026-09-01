package com.intern.trustai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intern.trustai.entity.BlockchainProof;
import com.intern.trustai.repository.BlockchainProofRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.messaging.MessageHeaders;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BlockchainAnchorService {

    private final BlockchainService blockchainService;
    private final BlockchainProofRepository proofRepository;
    private final Web3j web3j;
    private final ObjectMapper objectMapper;

    public BlockchainAnchorService(BlockchainService blockchainService, BlockchainProofRepository proofRepository, Web3j web3j) {
        this.blockchainService = blockchainService;
        this.proofRepository = proofRepository;
        this.web3j = web3j;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = {"rag-interactions", "audit-results", "hallucination-checks"}, groupId = "blockchain-anchor-group")
    public void listenAndAnchor(String message, MessageHeaders headers) {
        try {
            String topic = headers.get("kafka_receivedTopic", String.class);
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            
            // Format metadata based on topic
            String metadata = "Event: " + topic;
            String userId = "system";
            String eventId = "unknown";

            if (eventData.containsKey("userId")) userId = String.valueOf(eventData.get("userId"));
            else if (eventData.containsKey("auditor")) userId = String.valueOf(eventData.get("auditor"));

            if (eventData.containsKey("messageId")) eventId = String.valueOf(eventData.get("messageId"));
            else if (eventData.containsKey("contractId")) eventId = String.valueOf(eventData.get("contractId"));

            // Store in blockchain
            String txHash = blockchainService.storeAuditProof(message, metadata);

            // Save pending proof to DB
            BlockchainProof proof = new BlockchainProof();
            proof.setEventId(eventId);
            proof.setEventType(topic);
            proof.setTxHash(txHash);
            proof.setTimestamp(System.currentTimeMillis());

            proof.setStatus("skipped".equals(txHash) ? "FAILED" : "PENDING");
            proof.setUserId(userId);
            proof.setPayload(message);
            
            proofRepository.save(proof);

        } catch (Exception e) {
            System.err.println("Error anchoring event to blockchain: " + e.getMessage());
        }
    }

    // Poll every 5 seconds for PENDING transactions to check their block number
    @Scheduled(fixedDelay = 5000)
    public void checkPendingTransactions() {
        List<BlockchainProof> pendingProofs = proofRepository.findByStatus("PENDING");
        for (BlockchainProof proof : pendingProofs) {
            if (proof.getTxHash() == null || "skipped".equals(proof.getTxHash())) continue;
            
            try {
                Optional<TransactionReceipt> receiptOpt = web3j.ethGetTransactionReceipt(proof.getTxHash()).send().getTransactionReceipt();
                if (receiptOpt.isPresent()) {
                    TransactionReceipt receipt = receiptOpt.get();
                    if (receipt.getBlockNumber() != null) {
                        proof.setBlockNumber(receipt.getBlockNumber().longValue());
                        proof.setStatus("0x1".equals(receipt.getStatus()) ? "CONFIRMED" : "FAILED");
                        proofRepository.save(proof);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching transaction receipt for " + proof.getTxHash() + ": " + e.getMessage());
            }
        }
    }
}
