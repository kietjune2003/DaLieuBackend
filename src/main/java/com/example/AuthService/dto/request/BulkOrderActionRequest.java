package com.example.AuthService.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BulkOrderActionRequest {
    private List<Long> orderIds;
}
