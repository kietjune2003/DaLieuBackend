package com.example.AuthService.controller;

import com.example.AuthService.entity.Section;
import com.example.AuthService.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping // dùng absolute path ở method cho rõ ràng
public class SectionController {

    private final SectionService sectionService;

    /**
     * LIST sections theo thuốc (KHÔNG phân trang)
     * GET /api/drugs/{drugId}/sections
     */
    @GetMapping("/api/drugs/{drugId}/sections")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ResponseEntity<List<Section>> listByDrug(@PathVariable Long drugId) {
        return ResponseEntity.ok(sectionService.listByDrug(drugId));
    }

    /**
     * CREATE section cho 1 thuốc
     * POST /api/drugs/{drugId}/sections
     * Body: { "title": "...", "content": "..." }
     */
    @PostMapping("/api/drugs/{drugId}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Section> create(
            @PathVariable Long drugId,
            @RequestBody Section payload
    ) {
        return ResponseEntity.ok(sectionService.create(drugId, payload));
    }

    /**
     * GET one section
     * GET /api/sections/{id}
     */
    @GetMapping("/api/sections/{id}")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ResponseEntity<Section> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(sectionService.getById(id));
    }

    /**
     * UPDATE section (chỉ sửa title/content; không đổi drug)
     * PUT /api/sections/{id}
     * Body: { "title": "...", "content": "..." }
     */
    @PutMapping("/api/sections/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Section> update(
            @PathVariable Long id,
            @RequestBody Section payload
    ) {
        return ResponseEntity.ok(sectionService.update(id, payload));
    }

    /**
     * DELETE section
     * DELETE /api/sections/{id}
     */
    @DeleteMapping("/api/sections/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * (TÙY CHỌN) LIST tất cả sections (KHÔNG phân trang)
     * GET /api/sections
     */
    @GetMapping("/api/sections")
    @PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")
    public ResponseEntity<List<Section>> listAll() {
        return ResponseEntity.ok(sectionService.listAll());
    }
}
