package com.intern.trustai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private List<String> roles;
}
