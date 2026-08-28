package com.intern.trustai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "blockchain_proofs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(nullable = false)
    private Long timestamp;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, FAILED

    @Column(name = "user_id")
    private String userId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;
}
