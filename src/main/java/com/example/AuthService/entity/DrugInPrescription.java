package com.example.AuthService.entity;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.AuthService.entity.BaseEntity;
import com.example.AuthService.enums.FrequencyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drug_in_prescriptions")
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class DrugInPrescription extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = true)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String drugName;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    private Unit unit;

    private String note;
    private LocalDate startDate;
    private LocalDate endDate;


    @Enumerated(EnumType.STRING)
    private FrequencyType frequencyType; // DAILY, INTERVAL, WEEKLY

    private Integer intervalDays;
    @ElementCollection
    @CollectionTable(name = "drug_days_of_week", joinColumns = @JoinColumn(name = "drug_in_prescription_id"))
    @Column(name = "day_of_week")
    private List<String> daysOfWeek = new ArrayList<>();

    @OneToMany(mappedBy = "drugInPrescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();
    private Integer status = 1; //active


}
