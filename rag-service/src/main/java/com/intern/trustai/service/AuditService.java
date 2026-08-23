package com.intern.trustai.service;

import com.intern.trustai.dto.ContractStructureDTO;
import com.intern.trustai.entity.SmartContract;
import com.intern.trustai.repository.SmartContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuditService {

    private final SmartContractRepository smartContractRepository;

    public AuditService(SmartContractRepository smartContractRepository) {
        this.smartContractRepository = smartContractRepository;
    }

    public ContractStructureDTO uploadAndParseContract(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Save to DB
        SmartContract contract = new SmartContract();
        contract.setName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "Unknown.sol");
        contract.setContent(content);
        contract = smartContractRepository.save(contract);

        // Parse with Regex
        ContractStructureDTO dto = parseSolidityCode(content);
        dto.setContractId(contract.getId());
        return dto;
    }

    private ContractStructureDTO parseSolidityCode(String code) {
        ContractStructureDTO dto = new ContractStructureDTO();
        dto.setFunctions(new ArrayList<>());
        dto.setModifiers(new ArrayList<>());
        dto.setEvents(new ArrayList<>());
        dto.setStateVariables(new ArrayList<>());

        // Remove single line comments
        String cleanCode = code.replaceAll("//.*", "");
        // Remove multi-line comments
        cleanCode = cleanCode.replaceAll("/\\*[\\s\\S]*?\\*/", "");

        // 1. Contract Name
        Pattern contractPattern = Pattern.compile("contract\\s+([a-zA-Z0-9_]+)");
        Matcher contractMatcher = contractPattern.matcher(cleanCode);
        if (contractMatcher.find()) {
            dto.setContractName(contractMatcher.group(1));
        } else {
            dto.setContractName("UnknownContract");
        }

        // 2. Functions
        Pattern functionPattern = Pattern.compile("function\\s+([a-zA-Z0-9_]+\\s*\\([^)]*\\)[^{]*)");
        Matcher functionMatcher = functionPattern.matcher(cleanCode);
        while (functionMatcher.find()) {
            dto.getFunctions().add("function " + functionMatcher.group(1).trim());
        }

        // 3. Modifiers
        Pattern modifierPattern = Pattern.compile("modifier\\s+([a-zA-Z0-9_]+\\s*\\([^)]*\\)?)");
        Matcher modifierMatcher = modifierPattern.matcher(cleanCode);
        while (modifierMatcher.find()) {
            dto.getModifiers().add("modifier " + modifierMatcher.group(1).trim());
        }

        // 4. Events
        Pattern eventPattern = Pattern.compile("event\\s+([a-zA-Z0-9_]+\\s*\\([^)]*\\));");
        Matcher eventMatcher = eventPattern.matcher(cleanCode);
        while (eventMatcher.find()) {
            dto.getEvents().add("event " + eventMatcher.group(1).trim());
        }

        // 5. State Variables (Heuristic)
        Pattern stateVarPattern = Pattern.compile("(?:uint|int|string|address|bool|mapping|bytes)[\\w\\s]*?(?:public|private|internal|external)?[\\w\\s]*?[a-zA-Z0-9_]+\\s*;");
        Matcher stateVarMatcher = stateVarPattern.matcher(cleanCode);
        while (stateVarMatcher.find()) {
            String match = stateVarMatcher.group(0).trim();
            if (!match.contains("memory") && !match.contains("calldata")) {
                 dto.getStateVariables().add(match);
            }
        }

        return dto;
    }
}
