package com.intern.trustai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intern.trustai.entity.AuditFinding;
import com.intern.trustai.entity.SmartContract;
import com.intern.trustai.repository.AuditFindingRepository;
import com.intern.trustai.repository.SmartContractRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityAuditService {

    private final ChatLanguageModel chatLanguageModel;
    private final FindingValidationRuleEngine ruleEngine;
    private final AuditFindingRepository findingRepository;
    private final SmartContractRepository smartContractRepository;
    private final ObjectMapper objectMapper;

    public SecurityAuditService(ChatLanguageModel chatLanguageModel,
                                FindingValidationRuleEngine ruleEngine,
                                AuditFindingRepository findingRepository,
                                SmartContractRepository smartContractRepository) {
        this.chatLanguageModel = chatLanguageModel;
        this.ruleEngine = ruleEngine;
        this.findingRepository = findingRepository;
        this.smartContractRepository = smartContractRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public List<AuditFinding> runSecurityAudit(Long contractId, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        SmartContract contract = smartContractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        List<String> functions = extractFunctions(contract.getContent());
        List<AuditFinding> findings = new ArrayList<>();

        int total = functions.size();
        int current = 0;

        for (String func : functions) {
            current++;
            sendSseEvent(emitter, "Analyzing function " + current + " of " + total + "...");
            
            String prompt = "You are a Smart Contract Security Auditor. Analyze the following Solidity function for vulnerabilities.\n" +
                    "Function Code:\n" + func + "\n\n" +
                    "If you find vulnerabilities, output them strictly in this JSON array format with no markdown:\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"category\": \"Reentrancy | Authorization | Timestamp | Denial of Service | Injection | Compiler | Arithmetic | Access Control\",\n" +
                    "    \"severity\": \"CRITICAL | HIGH | MEDIUM | LOW\",\n" +
                    "    \"title\": \"Brief title\",\n" +
                    "    \"description\": \"Detailed description\",\n" +
                    "    \"codeSnippet\": \"The exact line(s) of code causing the issue\"\n" +
                    "  }\n" +
                    "]\n" +
                    "If no vulnerabilities are found, output an empty array: []";

            String response = chatLanguageModel.generate(prompt);

            // Extract JSON array using regex in case Llama3 adds conversational text
            Matcher jsonMatcher = Pattern.compile("\\[\\s*\\{.*?\\}\\s*\\]", Pattern.DOTALL).matcher(response);
            if (jsonMatcher.find()) {
                response = jsonMatcher.group(0);
            } else {
                // If it couldn't find a JSON array, default to empty array
                response = "[]";
            }

            try {
                JsonNode root = objectMapper.readTree(response);
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        String category = node.has("category") ? node.get("category").asText() : "Unknown";
                        String severity = node.has("severity") ? node.get("severity").asText() : "LOW";
                        String title = node.has("title") ? node.get("title").asText() : "Finding";
                        String description = node.has("description") ? node.get("description").asText() : "";
                        String codeSnippet = node.has("codeSnippet") ? node.get("codeSnippet").asText() : func;

                        sendSseEvent(emitter, "Validating finding: " + title + "...");
                        boolean isValid = ruleEngine.validateFinding(category, codeSnippet);

                        AuditFinding finding = new AuditFinding();
                        finding.setSmartContract(contract);
                        finding.setSeverity(severity);
                        finding.setTitle(title);
                        finding.setDescription(description);
                        finding.setCodeSnippet(codeSnippet);
                        finding.setValidatedByRules(isValid);

                        findings.add(findingRepository.save(finding));
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to parse LLM finding JSON: " + response);
                sendSseEvent(emitter, "Error parsing LLM response for a function. Raw output logged to server.");
            }
        }
        
        sendSseEvent(emitter, "Analysis complete. " + findings.size() + " findings saved.");
        return findings;
    }

    private void sendSseEvent(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String message) {
        if (emitter != null) {
            try {
                emitter.send(message);
            } catch (Exception e) {
                // Ignore broken pipe
            }
        }
    }

    private List<String> extractFunctions(String code) {
        List<String> funcs = new ArrayList<>();
        String cleanCode = code.replaceAll("//.*", "").replaceAll("/\\*[\\s\\S]*?\\*/", "");
        Pattern functionPattern = Pattern.compile("function\\s+([a-zA-Z0-9_]+\\s*\\([^)]*\\)[^{]*)");
        Matcher functionMatcher = functionPattern.matcher(cleanCode);
        while (functionMatcher.find()) {
            // For a real AST this would get the full body. For the prompt, we pass the signature and we should ideally pass the body.
            // Since Regex for matching brackets in Java is hard, we will just use the naive regex which captures signature.
            // Wait, we need the body to find vulnerabilities! 
            // We can split the string on 'function ' to approximate it.
            // Let's use a simpler heuristic for the demo:
            funcs.add("function " + functionMatcher.group(1));
        }
        
        // Better: let's just send the whole contract if it's small, or just approximate the functions.
        // For demonstration, let's just return the whole contract as one "function" if regex fails to get body, 
        // or actually split by "function " to get chunks.
        String[] parts = code.split("(?=\\bfunction\\b)");
        List<String> betterFuncs = new ArrayList<>();
        for(String part : parts) {
            if(part.trim().startsWith("function")) {
                betterFuncs.add(part);
            }
        }
        return betterFuncs.isEmpty() ? List.of(code) : betterFuncs;
    }
}
