package com.safehome.backend.domain.property;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.RegistrySnapshotNotFoundException;
import com.safehome.backend.domain.property.dto.PropertyOwnerCreateRequest;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PropertyOwnerService {
    private final PropertyOwnerRepository propertyOwnerRepository;
    private final PropertyRepository propertyRepository;
    private final RegistrySnapshotRepository snapshotRepository;
    private final OwnerIdentityService identityService;

    public PropertyOwnerService(PropertyOwnerRepository propertyOwnerRepository,
                                PropertyRepository propertyRepository,
                                RegistrySnapshotRepository snapshotRepository,
                                OwnerIdentityService identityService) {
        this.propertyOwnerRepository = propertyOwnerRepository;
        this.propertyRepository = propertyRepository;
        this.snapshotRepository = snapshotRepository;
        this.identityService = identityService;
    }

    public List<PropertyOwner> findAll() { return propertyOwnerRepository.findAll(); }

    public PropertyOwner findById(Long id) {
        return propertyOwnerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<PropertyOwner> findByPropertyId(Long propertyId) {
        return propertyOwnerRepository.findByPropertyId(propertyId);
    }

    public List<PropertyOwner> findByRegistrySnapshotId(Long snapshotId) {
        return propertyOwnerRepository.findByRegistrySnapshotId(snapshotId);
    }

    @Transactional
    public PropertyOwner create(PropertyOwnerCreateRequest request) {
        if (request == null || request.propertyId() == null
                || request.registrySnapshotId() == null || request.ownerName() == null
                || request.ownerName().isBlank() || request.ownerName().length() > 200) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new PropertyNotFoundException(request.propertyId()));
        RegistrySnapshot snapshot = snapshotRepository.findById(request.registrySnapshotId())
                .orElseThrow(() -> new RegistrySnapshotNotFoundException(request.registrySnapshotId()));
        if (!snapshot.getProperty().getId().equals(property.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validateOwnershipRatio(request.ownershipRatio());
        PropertyOwner owner = PropertyOwner.create(property, snapshot, request.ownerName().trim(),
                identityService.encryptMaskedIdentifier(request.ownerRrnMasked()),
                identityService.hashBirth6(request.ownerRrnMasked()),
                request.ownershipRatio(), request.registeredAt());
        return propertyOwnerRepository.save(owner);
    }

    @Transactional
    public PropertyOwner updateOwnershipRatio(Long id, BigDecimal ratio) {
        validateOwnershipRatio(ratio);
        PropertyOwner owner = findById(id);
        owner.updateOwnershipRatio(ratio);
        return owner;
    }

    @Transactional
    public void delete(Long id) { propertyOwnerRepository.delete(findById(id)); }

    private void validateOwnershipRatio(BigDecimal ratio) {
        if (ratio != null && (ratio.compareTo(BigDecimal.ZERO) <= 0
                || ratio.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
