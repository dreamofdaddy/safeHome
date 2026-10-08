package com.safehome.backend.domain.property;

import com.safehome.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_owners")
public class PropertyOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "property_owners_seq")
    @SequenceGenerator(
            name = "property_owners_seq",
            sequenceName = "property_owners_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ownership_ratio", precision = 5, scale = 2)
    private BigDecimal ownershipRatio;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PropertyOwner() {
    }

    private PropertyOwner(
            Property property,
            User user,
            BigDecimal ownershipRatio
    ) {
        this.property = property;
        this.user = user;
        this.ownershipRatio = ownershipRatio;
    }

    public static PropertyOwner create(
            Property property,
            User user,
            BigDecimal ownershipRatio
    ) {
        return new PropertyOwner(property, user, ownershipRatio);
    }

    public void updateOwnershipRatio(BigDecimal ownershipRatio) {
        this.ownershipRatio = ownershipRatio;
    }

    public void verify() {
        this.verifiedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getOwnershipRatio() {
        return ownershipRatio;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}