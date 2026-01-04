package com.example.AuthService.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkOrderActionResult {

    private int total;
    private int success;
    private int failed;

    private List<Long> successIds;
    private List<BulkOrderError> errors;
}
