package com.example.AuthService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionSummaryResponse {
    private Long id;
    private String prescriptionName;
    private Integer totalDrugs;
    private List<DrugSummaryResponse> drugs;
}
