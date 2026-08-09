package com.intern.trustai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HallucinationGuardService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public HallucinationGuardService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = new ObjectMapper();
    }

    public GuardResult verifyClaims(String context, String aiResponse) {
        String promptString = "You are a strict compliance auditor. Your job is to verify if the AI's response is fully supported by the provided source context.\n" +
                "Extract all factual claims from the AI Response.\n" +
                "For each claim, determine if it is explicitly supported by the Context (isSupported: true) or if it is a hallucination/unsupported (isSupported: false).\n" +
                "Calculate an overall confidenceScore from 0 to 100 based on the percentage of supported claims (100 if all supported, 0 if none).\n" +
                "You MUST output ONLY a valid JSON object in the exact format below, with no markdown formatting or extra text:\n" +
                "{\n" +
                "  \"confidenceScore\": 85,\n" +
                "  \"claims\": [\n" +
                "    { \"text\": \"claim 1\", \"isSupported\": true },\n" +
                "    { \"text\": \"claim 2\", \"isSupported\": false }\n" +
                "  ]\n" +
                "}\n\n" +
                "Context:\n{{context}}\n\n" +
                "AI Response:\n{{response}}";

        PromptTemplate promptTemplate = PromptTemplate.from(promptString);
        Map<String, Object> variables = new HashMap<>();
        variables.put("context", context);
        variables.put("response", aiResponse);

        String jsonResult = chatLanguageModel.generate(promptTemplate.apply(variables).text());
        
        // Clean up markdown if LLM adds ```json
        if (jsonResult.startsWith("```json")) {
            jsonResult = jsonResult.substring(7);
        } else if (jsonResult.startsWith("```")) {
            jsonResult = jsonResult.substring(3);
        }
        if (jsonResult.endsWith("```")) {
            jsonResult = jsonResult.substring(0, jsonResult.length() - 3);
        }
        jsonResult = jsonResult.trim();

        GuardResult result = new GuardResult();
        try {
            JsonNode node = objectMapper.readTree(jsonResult);
            result.setConfidenceScore(node.get("confidenceScore").asInt());
            result.setClaimAnalysis(jsonResult);
        } catch (Exception e) {
            e.printStackTrace();
            // Default fallback if JSON parsing fails
            result.setConfidenceScore(0);
            result.setClaimAnalysis("{\"confidenceScore\":0, \"claims\":[], \"error\":\"Failed to parse guard response\"}");
        }

        return result;
    }

    public static class GuardResult {
        private int confidenceScore;
        private String claimAnalysis;

        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getClaimAnalysis() { return claimAnalysis; }
        public void setClaimAnalysis(String claimAnalysis) { this.claimAnalysis = claimAnalysis; }
    }
}
