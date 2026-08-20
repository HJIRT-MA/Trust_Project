package com.intern.trustai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractStructureDTO {
    private String contractName;
    private List<String> functions;
    private List<String> modifiers;
    private List<String> events;
    private List<String> stateVariables;
}
