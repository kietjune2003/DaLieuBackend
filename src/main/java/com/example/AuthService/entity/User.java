package com.example.AuthService.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_role_id", columnList = "role_id"),
                @Index(name = "idx_users_country_id", columnList = "country_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"password", "role", "country"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false) // <-- KHÔNG gọi super
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 150)
    private String name;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(length = 255)
    private String password;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "facebook_account_id", length = 100)
    private String facebookAccountId;

    @Column(name = "google_account_id", length = 100)
    private String googleAccountId;

    // 1 user = 1 role
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    private String photoUrl;


    // ---- cờ trạng thái tài khoản (map vào DB) ----
    @Builder.Default private boolean accountNonExpired = true;
    @Builder.Default private boolean accountNonLocked = true;
    @Builder.Default private boolean credentialsNonExpired = true;
    @Builder.Default private boolean enabled = true;

    // Chuẩn hoá email
    @PrePersist @PreUpdate
    private void normalize() {
        if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
    }

    // ===== UserDetails =====
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == null || this.role.getName() == null) return Collections.emptyList();
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.getName().toUpperCase(Locale.ROOT)));
    }
    @Override public String getUsername() { return this.email; }
    @Override public boolean isAccountNonExpired()    { return accountNonExpired; }
    @Override public boolean isAccountNonLocked()     { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired(){ return credentialsNonExpired; }
    @Override public boolean isEnabled()              { return enabled; }

    // convenience ctor cho social login
    public User(String googleAccountId, String email, String name, String photoUrl) {
        this.googleAccountId = googleAccountId;
        this.email = email;
        this.name = name;
        this.photoUrl = photoUrl;
        this.enabled = true;
    }
}
