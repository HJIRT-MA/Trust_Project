package com.intern.trustai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReliabilityStatItem {
    private String date;
    private Double averageScore;
}
