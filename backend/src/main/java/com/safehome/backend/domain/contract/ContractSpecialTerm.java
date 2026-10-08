package com.safehome.backend.domain.contract;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_special_terms")
public class ContractSpecialTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "term_type", nullable = false, length = 50)
    private String termType;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ContractSpecialTerm() {
    }

    public ContractSpecialTerm(
            Long contractId,
            String termType,
            String content,
            Integer sequence
    ) {
        this.contractId = contractId;
        this.termType = termType;
        this.content = content;
        this.sequence = sequence;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (sequence == null) {
            sequence = 0;
        }

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

    public Long getContractId() {
        return contractId;
    }

    public String getTermType() {
        return termType;
    }

    public String getContent() {
        return content;
    }

    public Integer getSequence() {
        return sequence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String termType,
            String content,
            Integer sequence
    ) {
        this.termType = termType;
        this.content = content;
        this.sequence = sequence;
    }
}