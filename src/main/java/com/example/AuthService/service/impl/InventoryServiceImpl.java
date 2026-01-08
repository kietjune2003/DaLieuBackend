package com.example.AuthService.service.impl;

import com.example.AuthService.entity.Drug;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.repository.ImportInvoiceDetailRepository;
import com.example.AuthService.repository.OrderItemRepository;
import com.example.AuthService.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ImportInvoiceDetailRepository importRepo;
    private final OrderItemRepository orderItemRepo;
    private final DrugRepository drugRepository;
    @Override
    public Integer calculateStock(Long drugId) {
        Integer imported = importRepo.totalImported(drugId);
        Drug drug = drugRepository.findById(drugId).orElseThrow(() -> new RuntimeException("Drug not found"));
        Integer sold = drug.getSoldQuantity();
        return imported - sold;
    }

    @Override
    public Map<Long, Integer> calculateStockForDrugs(List<Long> drugIds) {
        return Map.of();
    }

    @Override
    public Map<Long, Integer> getTotalImportedForDrugs(List<Long> drugIds) {

        List<Object[]> results =
                importRepo.findTotalImportedForDrugs(drugIds);

        Map<Long, Integer> importedMap = new HashMap<>();

        for (Object[] row : results) {
            Long drugId = (Long) row[0];
            Long totalImported = (Long) row[1];

            importedMap.put(drugId, totalImported.intValue());
        }

        return importedMap;
    }




}
