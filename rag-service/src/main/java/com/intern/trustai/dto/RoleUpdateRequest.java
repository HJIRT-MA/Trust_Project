package com.intern.trustai.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoleUpdateRequest {
    private List<String> roles;
}
