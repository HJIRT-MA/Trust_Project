package com.intern.trustai.service;

import java.util.Map;

public interface SwcRagService {

    Map<String, String> enrichFinding(String findingTitle, String findingDescription, String functionCode);
}
