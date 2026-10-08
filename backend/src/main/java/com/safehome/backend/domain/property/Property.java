package com.safehome.backend.domain.property;

//import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "properties_seq"
    )
    @SequenceGenerator(
        name = "properties_seq",
        sequenceName = "properties_id_seq",
        allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "road_address", length = 500)
    private String roadAddress;

    @Column(name = "building_name", length = 200)
    private String buildingName;

    @Column(name = "building_type", length = 50)
    private String buildingType;

    @Column(name = "unit_number", length = 100)
    private String unitNumber;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
        mappedBy = "property",
        fetch = FetchType.LAZY
    )
    private List<PropertyOwner> owners = new ArrayList<>();

    protected Property() {
    }

    public Property(
        String address,
        String roadAddress,
        String buildingName,
        String buildingType,
        String unitNumber,
        String postalCode
    ) {
        this.address = address;
        this.roadAddress = roadAddress;
        this.buildingName = buildingName;
        this.buildingType = buildingType;
        this.unitNumber = unitNumber;
        this.postalCode = postalCode;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<PropertyOwner> getOwners() {
        return owners;
    }

    public void update(
        String address,
        String roadAddress,
        String buildingName,
        String buildingType,
        String unitNumber,
        String postalCode
    ) {
        this.address = address;
        this.roadAddress = roadAddress;
        this.buildingName = buildingName;
        this.buildingType = buildingType;
        this.unitNumber = unitNumber;
        this.postalCode = postalCode;
    }
}