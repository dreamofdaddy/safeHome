package com.safehome.backend.domain.registry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "registry_rights")
public class RegistryRight {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "registry_rights_seq"
    )
    @SequenceGenerator(
            name = "registry_rights_seq",
            sequenceName = "registry_rights_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registry_snapshot_id", nullable = false)
    private RegistrySnapshot registrySnapshot;

    @Column(name = "right_type", nullable = false, length = 50)
    private String rightType;

    @Column(name = "holder_name", length = 200)
    private String holderName;

    @Column(name = "amount", precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RegistryRight() {
    }

    private RegistryRight(
            RegistrySnapshot registrySnapshot,
            String rightType,
            String holderName,
            BigDecimal amount,
            Integer priority,
            LocalDateTime registeredAt,
            String status
    ) {
        this.registrySnapshot = registrySnapshot;
        this.rightType = rightType;
        this.holderName = holderName;
        this.amount = amount;
        this.priority = priority;
        this.registeredAt = registeredAt;
        this.status = status;
    }

    public static RegistryRight create(
            RegistrySnapshot registrySnapshot,
            String rightType,
            String holderName,
            BigDecimal amount,
            Integer priority,
            LocalDateTime registeredAt,
            String status
    ) {
        return new RegistryRight(
                registrySnapshot,
                rightType,
                holderName,
                amount,
                priority,
                registeredAt,
                status
        );
    }

    public void deactivate(String inactiveStatus) {
        this.status = inactiveStatus;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public RegistrySnapshot getRegistrySnapshot() {
        return registrySnapshot;
    }

    public String getRightType() {
        return rightType;
    }

    public String getHolderName() {
        return holderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getPriority() {
        return priority;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}