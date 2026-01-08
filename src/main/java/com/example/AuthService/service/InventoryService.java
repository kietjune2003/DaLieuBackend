package com.example.AuthService.service;

import java.util.List;
import java.util.Map;

public interface InventoryService {
    Integer calculateStock(Long drugId);

    Map<Long, Integer> calculateStockForDrugs(List<Long> drugIds);

    Map<Long, Integer> getTotalImportedForDrugs(List<Long> drugIds);
}
