package com.safehome.backend.domain.property;

import com.safehome.backend.domain.registry.RegistrySnapshot;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_owners")
public class PropertyOwner {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "property_owners_seq")
    @SequenceGenerator(name = "property_owners_seq", sequenceName = "property_owners_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registry_snapshot_id", nullable = false)
    private RegistrySnapshot registrySnapshot;

    @Column(name = "owner_name", nullable = false, length = 200)
    private String ownerName;

    @Column(name = "owner_rrn_encrypted")
    private byte[] ownerRrnEncrypted;

    @Column(name = "owner_rrn_birth6_hash", length = 64)
    private String ownerRrnBirth6Hash;

    @Column(name="owner_entity_type",nullable=false,length=20)
    private String ownerEntityType="UNKNOWN";
    @Column(name="corporate_registration_encrypted")
    private byte[] corporateRegistrationEncrypted;
    public String getOwnerEntityType(){return ownerEntityType;}
    public static PropertyOwner createCorporation(Property property,RegistrySnapshot snapshot,String name,
            byte[] encrypted,BigDecimal ratio,LocalDateTime registeredAt) {
        var owner=new PropertyOwner();owner.property=property;owner.registrySnapshot=snapshot;owner.ownerName=name;
        owner.ownerEntityType="CORPORATION";owner.corporateRegistrationEncrypted=encrypted==null?null:encrypted.clone();
        owner.ownershipRatio=ratio;owner.registeredAt=registeredAt;return owner;
    }

    @Column(name = "ownership_ratio", precision = 12, scale = 8)
    private BigDecimal ownershipRatio;
    @Column(name = "ownership_share", length = 100)
    private String ownershipShare;
    @Column(name="ownership_type",length=20) private String ownershipType="UNKNOWN";
    @Column(name="registry_priority_number",length=40) private String registryPriorityNumber;
    @Column(name="owner_address",length=1000) private String ownerAddress;
    @Column(name="acquisition_amount",precision=20,scale=2) private BigDecimal acquisitionAmount;
    public String getOwnershipType(){return ownershipType;}
    public String getRegistryPriorityNumber(){return registryPriorityNumber;}
    public String getOwnerAddress(){return ownerAddress;}
    public BigDecimal getAcquisitionAmount(){return acquisitionAmount;}
    public void preserveSourceEvidence(String kind,String priority,String address,BigDecimal amount){
        ownershipType=kind==null?"UNKNOWN":kind;registryPriorityNumber=priority;ownerAddress=address;acquisitionAmount=amount;
    }
    public String getOwnershipShare() { return ownershipShare; }
    public void preserveOwnershipShare(String share) { ownershipShare=share; }

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PropertyOwner() {}

    public static PropertyOwner create(Property property, RegistrySnapshot snapshot,
                                       String ownerName, byte[] encryptedIdentifier,
                                       String birth6Hash, BigDecimal ownershipRatio,
                                       LocalDateTime registeredAt) {
        PropertyOwner owner = new PropertyOwner();
        owner.property = property;
        owner.registrySnapshot = snapshot;
        owner.ownerName = ownerName;
        owner.ownerEntityType = "PERSON";
        owner.ownerRrnEncrypted = encryptedIdentifier.clone();
        owner.ownerRrnBirth6Hash = birth6Hash;
        owner.ownershipRatio = ownershipRatio;
        owner.registeredAt = registeredAt;
        return owner;
    }

    public void updateOwnershipRatio(BigDecimal ratio) { ownershipRatio = ratio; }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Property getProperty() { return property; }
    public RegistrySnapshot getRegistrySnapshot() { return registrySnapshot; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerRrnBirth6Hash() { return ownerRrnBirth6Hash; }
    public BigDecimal getOwnershipRatio() { return ownershipRatio; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
