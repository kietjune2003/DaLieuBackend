package com.example.AuthService.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drugs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Drug {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String title;
    private String image;

    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    private BigDecimal importPrice;
//    @Column(nullable = false)
//    private Integer stockQuantity;
    @Column(nullable = false)
    @Builder.Default
    private Integer soldQuantity = 0;
    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @OneToMany(mappedBy = "drug", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Section> sections = new ArrayList<>();
    private Boolean isActive = true;
}
