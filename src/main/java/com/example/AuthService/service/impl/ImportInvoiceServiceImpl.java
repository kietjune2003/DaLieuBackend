package com.example.AuthService.service.impl;

import com.example.AuthService.dto.request.ImportInvoiceDetailRequest;
import com.example.AuthService.dto.request.ImportInvoiceRequest;
import com.example.AuthService.dto.response.ImportInvoiceDetailResponse;
import com.example.AuthService.dto.response.ImportInvoiceResponse;
import com.example.AuthService.entity.Drug;
import com.example.AuthService.entity.ImportInvoice;
import com.example.AuthService.entity.ImportInvoiceDetail;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.repository.ImportInvoiceRepository;
import com.example.AuthService.service.ImportInvoiceService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportInvoiceServiceImpl implements ImportInvoiceService {

    private final ImportInvoiceRepository invoiceRepository;
    private final DrugRepository drugRepository;

    @Override
    public Page<ImportInvoiceResponse> getAll(
            String keyword,
            Pageable pageable
    ) {
        Page<ImportInvoice> page;

        if (keyword != null && !keyword.isBlank()) {
            page = invoiceRepository
                    .findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            page = invoiceRepository.findAll(pageable);
        }

        return page.map(this::toResponse);
    }
    @Override
    @Transactional
    public ImportInvoiceResponse importFromExcel(MultipartFile file) {
        try {
            String filename = Objects.requireNonNull(file.getOriginalFilename());
            String invoiceName = filename.replaceFirst("[.][^.]+$", "");

            ImportInvoice invoice = new ImportInvoice();
            invoice.setName(invoiceName);
            invoice.setCreatedAt(LocalDateTime.now());

            List<ImportInvoiceDetail> details = new ArrayList<>();

            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                final int rowIndex = i + 1; // 👈 FIX TẠI ĐÂY

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String drugName = formatter.formatCellValue(row.getCell(0)).trim();
                String quantityStr = formatter.formatCellValue(row.getCell(1)).trim();

                if (drugName.isEmpty() || quantityStr.isEmpty()) {
                    continue;
                }

                int quantity;
                try {
                    quantity = Integer.parseInt(quantityStr);
                } catch (NumberFormatException e) {
                    throw new RuntimeException(
                            "Số lượng không hợp lệ tại dòng " + rowIndex
                    );
                }

                Drug drug = drugRepository
                        .findByNameIgnoreCase(drugName)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy thuốc '" + drugName +
                                                "' tại dòng " + rowIndex
                                )
                        );

                ImportInvoiceDetail detail = new ImportInvoiceDetail();
                detail.setInvoice(invoice);
                detail.setDrug(drug);
                detail.setQuantity(quantity);

                details.add(detail);


            }


            invoice.setDetails(details);
            ImportInvoice saved = invoiceRepository.save(invoice);

            workbook.close();
            return toResponse(saved);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage(), e);
        }
    }



    @Override
    public ImportInvoiceResponse getById(Long id) {
        ImportInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        return toResponse(invoice);
    }

    @Override
    public ImportInvoiceResponse create(ImportInvoiceRequest request) {

        ImportInvoice invoice = new ImportInvoice();
        invoice.setName(request.getName());
        invoice.setCreatedAt(LocalDateTime.now());

        List<ImportInvoiceDetail> details = request.getDetails()
                .stream()
                .map(d -> {
                    Drug drug = drugRepository
                            .findByNameIgnoreCase(d.getDrugName())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Không tìm thấy thuốc: " + d.getDrugName()
                                    )
                            );



                    ImportInvoiceDetail detail = new ImportInvoiceDetail();
                    detail.setInvoice(invoice);
                    detail.setDrug(drug);
                    detail.setQuantity(d.getQuantity());

                    return detail;
                })
                .toList();

        invoice.setDetails(details);

        ImportInvoice saved = invoiceRepository.save(invoice);
        return toResponse(saved);
    }


    @Override
    public void delete(Long id) {
        invoiceRepository.deleteById(id);
    }

    /* ================= MAPPER ================= */

    private ImportInvoiceResponse toResponse(ImportInvoice invoice) {
        ImportInvoiceResponse res = new ImportInvoiceResponse();
        res.setId(invoice.getId());
        res.setName(invoice.getName());
        res.setCreatedAt(invoice.getCreatedAt());

        List<ImportInvoiceDetailResponse> detailResponses =
                invoice.getDetails()
                        .stream()
                        .map(d -> {
                            ImportInvoiceDetailResponse dr =
                                    new ImportInvoiceDetailResponse();
                            dr.setId(d.getId());
                            dr.setDrugId(d.getDrug().getId());
                            dr.setDrugName(d.getDrug().getName());
                            dr.setQuantity(d.getQuantity());
                            return dr;
                        })
                        .toList();

        res.setDetails(detailResponses);
        return res;
    }
}

