package com.example.AuthService.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PrescriptionRequest {
    private String name;
    private List<DrugInPresRequest> drugs;
    private String hospital;
    private String doctorName;
    private String consultationDate;
    private String followUpDate;
}