package com.example.AuthService.service.impl;

import com.example.AuthService.entity.Section;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.repository.SectionRepository;
import com.example.AuthService.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final DrugRepository drugRepository;

    @Override
    public List<Section> listByDrug(Long drugId) {
        // Đảm bảo thuốc tồn tại (tránh trả về mảng rỗng cho id không hợp lệ)
        if (!drugRepository.existsById(drugId)) {
            throw new RuntimeException("Drug not found");
        }
        return sectionRepository.findByDrugIdOrderByIdAsc(drugId);
    }

    @Override
    @Transactional
    public Section create(Long drugId, Section payload) {
        var drug = drugRepository.findById(drugId)
                .orElseThrow(() -> new RuntimeException("Drug not found"));

        Section s = new Section();
        s.setTitle(payload != null ? payload.getTitle() : null);
        s.setContent(payload != null ? payload.getContent() : null);
        // BẮT BUỘC gán đúng drug theo path, bỏ qua drug trong body nếu có
        s.setDrug(drug);

        return sectionRepository.save(s);
    }

    @Override
    public Section getById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found"));
    }

    @Override
    @Transactional
    public Section update(Long id, Section payload) {
        Section existing = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        // Chỉ cho phép sửa title/content; KHÔNG đổi drug
        if (payload != null) {
            if (payload.getTitle() != null) existing.setTitle(payload.getTitle());
            if (payload.getContent() != null) existing.setContent(payload.getContent());
        }
        return sectionRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!sectionRepository.existsById(id)) {
            throw new RuntimeException("Section not found");
        }
        sectionRepository.deleteById(id);
    }

    @Override
    public List<Section> listAll() {
        return sectionRepository.findAllByOrderByIdAsc();
    }
}
