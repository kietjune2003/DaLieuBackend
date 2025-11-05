package com.example.AuthService.controller;

import com.example.AuthService.dto.DrugFilter;
import com.example.AuthService.entity.Drug;
import com.example.AuthService.service.DrugService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    /**
     * Phân trang + lọc + sắp xếp
     * Ví dụ:
     *  GET /api/drugs?page=0&size=20&sort=id,desc&q=para&minPrice=10000&maxPrice=50000&inStock=true&hasImage=true
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<Drug>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean hasImage,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        DrugFilter f = new DrugFilter();
        f.setQ(q);
        f.setMinPrice(minPrice);
        f.setMaxPrice(maxPrice);
        f.setInStock(inStock);
        f.setHasImage(hasImage);
        return ResponseEntity.ok(drugService.getDrugs(f, pageable));
    }

    /**
     * Gợi ý autocomplete cho ô search
     * Ví dụ:
     *  GET /api/drugs/suggest?q=para&limit=10
     */
    @GetMapping("/suggest")
    @PreAuthorize("hasAnyAuthority('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<String>> suggest(@RequestParam String q,
                                                @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(drugService.suggestNames(q, limit));
    }

    /**
     * Trả toàn bộ danh sách (không phân trang) — đổi path thành /all để tránh trùng @GetMapping("/")
     * Chỉ dùng khi thật sự cần (danh sách nhỏ).
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Drug>> getAllDrugs() {
        return ResponseEntity.ok(drugService.getAllDrugs());
    }

    // READ by id
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Drug> getDrug(@PathVariable Long id) {
        return ResponseEntity.ok(drugService.getDrugById(id));
    }

    // CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Drug> createDrug(@RequestBody Drug drug) {
        return ResponseEntity.ok(drugService.createDrug(drug));
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Drug> updateDrug(@PathVariable Long id, @RequestBody Drug drug) {
        return ResponseEntity.ok(drugService.updateDrug(id, drug));
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteDrug(@PathVariable Long id) {
        drugService.deleteDrug(id);
        return ResponseEntity.noContent().build();
    }
}
