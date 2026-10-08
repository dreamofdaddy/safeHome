package com.safehome.backend.domain.registry;

import com.safehome.backend.domain.property.Property;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "registry_snapshots")
public class RegistrySnapshot {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "registry_snapshots_seq"
    )
    @SequenceGenerator(
            name = "registry_snapshots_seq",
            sequenceName = "registry_snapshots_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "document_hash", length = 128)
    private String documentHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RegistrySnapshot() {
    }

    private RegistrySnapshot(
            Property property,
            String source,
            LocalDateTime observedAt,
            String documentHash
    ) {
        this.property = property;
        this.source = source;
        this.observedAt = observedAt;
        this.documentHash = documentHash;
    }

    public static RegistrySnapshot create(
            Property property,
            String source,
            LocalDateTime observedAt,
            String documentHash
    ) {
        return new RegistrySnapshot(
                property,
                source,
                observedAt,
                documentHash
        );
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

    public Property getProperty() {
        return property;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}